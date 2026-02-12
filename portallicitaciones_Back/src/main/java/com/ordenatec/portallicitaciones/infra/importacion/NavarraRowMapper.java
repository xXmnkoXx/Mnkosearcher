package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.ordenatec.portallicitaciones.domain.enums.EstadoLicitacion;
import com.ordenatec.portallicitaciones.domain.enums.TipoContrato;
import com.ordenatec.portallicitaciones.domain.model.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Mapper Navarra (datosabiertos.navarra.es datastore dump JSON)
 *
 * fields:
 *  _id, Entidad, Organo, Modalidad, TipoContrato, Procedimiento, CriterioAdjudicacion,
 *  BreveDescripcion, PrecioLicitacion, ValorEstimado, FechaPublicacion,
 *  codigoNUTS, LugarEjecucion, Estado, Motivo, PlazoEjecucion, CPV
 *
 * records: array de arrays (posiciones según fields)
 */
@Component
public class NavarraRowMapper {

    private static final DateTimeFormatter NAV_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Map<String, Integer> buildIndex(JsonNode fieldsArray) {
        Map<String, Integer> idx = new HashMap<>();
        if (fieldsArray == null || !fieldsArray.isArray()) return idx;

        int i = 0;
        for (JsonNode f : fieldsArray) {
            String id = (f != null && f.hasNonNull("id")) ? f.get("id").asText() : null;
            if (id != null && !id.isBlank()) idx.put(id, i);
            i++;
        }
        return idx;
    }

    /** Fecha dd/MM/yyyy (Navarra) con fallback ISO. */
    public LocalDate getFecha(Map<String, Integer> idx, JsonNode rec, String fieldName) {
        String s = getText(idx, rec, fieldName);
        if (s == null) return null;

        try { return LocalDate.parse(s, NAV_DATE); } catch (Exception ignore) {}
        try { return LocalDate.parse(s); } catch (Exception ignore) {}

        return null;
    }

    public Licitacion toLicitacion(Map<String, Integer> idx, JsonNode rec) {
        if (rec == null || !rec.isArray()) return null;

        // _id es int en el dump; lo tratamos como string estable
        String navId = getText(idx, rec, "_id");
        if (navId == null) return null;

        Licitacion lic = new Licitacion();
        lic.setId(UUID.randomUUID());

        // Expediente estable
        lic.setExpediente("NAVARRA-" + navId);

        // Título
        String titulo = firstNonBlank(
                getText(idx, rec, "BreveDescripcion"),
                getText(idx, rec, "Modalidad"),
                getText(idx, rec, "TipoContrato")
        );
        lic.setTitulo(titulo);

        // ✅ Navarra: URL genérica al buscador de anuncios (no hay URL de detalle en el dump)
        String navarraGenericUrl = "https://hacienda.navarra.es/sicpportal/mtoBuscadorAnuncios.aspx";
        lic.setUrlPublica(navarraGenericUrl);
        // Si tu entidad tiene también url_detalle, lo rellenamos
        setIfPresent(lic, "setUrlDetalle", String.class, navarraGenericUrl);

        // Estado
        String estadoTxt = getText(idx, rec, "Estado");
        lic.setEstado(mapEstado(estadoTxt));
        // Guardar texto bruto en BD si existe el campo (estado_texto)
        setIfPresent(lic, "setEstadoTexto", String.class, estadoTxt);

        // Procedimiento / Modalidad / Criterio
        String procedimiento = getText(idx, rec, "Procedimiento");
        String modalidad = getText(idx, rec, "Modalidad");
        String criterio = getText(idx, rec, "CriterioAdjudicacion");

        lic.setProcedimiento(procedimiento);

        // Si tu dominio/entidad tiene estos campos, se rellenan.
        setIfPresent(lic, "setModalidad", String.class, modalidad);
        setIfPresent(lic, "setCriterioAdjudicacion", String.class, criterio);

        // TipoContrato
        String tipoTxt = getText(idx, rec, "TipoContrato");
        lic.setTipoContrato(mapTipoContrato(tipoTxt));
        // texto en BD (tipo_contrato_texto)
        setIfPresent(lic, "setTipoContratoTexto", String.class, tipoTxt);

        // Fechas
        if (lic.getFechas() == null) lic.setFechas(new FechasProcedimiento());
        LocalDate fpub = getFecha(idx, rec, "FechaPublicacion");
        lic.getFechas().setFechaPublicacion(fpub);

        // En dump no hay "updated" real
        lic.setLastEventUpdatedAt(Instant.now());
        lic.setLastEventEntryId("NAV-DUMP-" + navId);

        // Importes
        String precio = getText(idx, rec, "PrecioLicitacion");
        String ve = getText(idx, rec, "ValorEstimado");

        BigDecimal precioBd = parseDecimalEs(precio);
        BigDecimal veBd = parseDecimalEs(ve);

        if (precioBd != null) lic.setPresupuestoBase(toMoney("EUR", precioBd, null));
        if (veBd != null) lic.setValorEstimado(toMoney("EUR", veBd, null));
        lic.setMoneda("EUR");

        // Si tienes columnas planas en BD (precio_licitacion / valor_estimado), también las seteamos si existen
        setIfPresent(lic, "setPrecioLicitacion", BigDecimal.class, precioBd);
        setIfPresent(lic, "setValorEstimadoImporte", BigDecimal.class, veBd); // por si usas otro nombre
        setIfPresent(lic, "setValorEstimadoBd", BigDecimal.class, veBd);       // por si usas otro nombre

        // NUTS / lugar ejecución / motivo / plazo ejecución
        String nuts = getText(idx, rec, "codigoNUTS");
        String lugar = getText(idx, rec, "LugarEjecucion");
        String motivo = getText(idx, rec, "Motivo");
        String plazo = getText(idx, rec, "PlazoEjecucion");

        setIfPresent(lic, "setCodigoNuts", String.class, nuts);
        setIfPresent(lic, "setCodigoNUTS", String.class, nuts); // variantes
        setIfPresent(lic, "setLugarEjecucion", String.class, lugar);
        setIfPresent(lic, "setMotivo", String.class, motivo);
        setIfPresent(lic, "setPlazoEjecucion", String.class, plazo);

        // Organismo
        String entidad = getText(idx, rec, "Entidad");
        String organo = getText(idx, rec, "Organo");

        // Si tu BD tiene columnas entidad/organo planas
        setIfPresent(lic, "setEntidad", String.class, entidad);
        setIfPresent(lic, "setOrgano", String.class, organo);

        if (entidad != null || organo != null) {
            OrganismoContratacion o = new OrganismoContratacion();
            o.setNombre(firstNonBlank(organo, entidad));
            lic.setOrganismo(o);
        }

        // CPVs
        String cpvRaw = getText(idx, rec, "CPV");
        List<Cpv> cpvs = parseCpvs(cpvRaw);
        if (cpvs != null && !cpvs.isEmpty()) {
            if (lic.getCpvs() == null) lic.setCpvs(new ArrayList<>());
            lic.getCpvs().addAll(cpvs);
        }

        // Opcional: guardar también el "navarra_id" / "uuid" si tu tabla lo tiene
        setIfPresent(lic, "setUuid", String.class, navId);
        setIfPresent(lic, "setFuenteId", String.class, navId);
        setIfPresent(lic, "setExternalId", String.class, navId);

        return lic;
    }

    // ----------------- helpers lectura -----------------

    public String getText(Map<String, Integer> idx, JsonNode rec, String fieldName) {
        if (idx == null || rec == null || !rec.isArray() || fieldName == null) return null;
        Integer pos = idx.get(fieldName);
        if (pos == null) return null;
        JsonNode v = rec.get(pos);
        if (v == null || v.isNull()) return null;

        String s = v.asText();
        s = (s != null) ? s.trim() : null;
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    // "1.234,56" / "4478039,51" / "108000,00"
    private static BigDecimal parseDecimalEs(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isBlank()) return null;

        t = t.replace(".", "")
             .replace(" ", "")
             .replace("\u00A0", "");
        t = t.replace(",", ".");
        try { return new BigDecimal(t); } catch (Exception ignore) { return null; }
    }

    private static Money toMoney(String currency, BigDecimal amount, Boolean incluyeIva) {
        if (amount == null) return null;
        Money m = new Money();
        m.setAmount(amount);
        m.setCurrency(currency);
        m.setIncluyeIVA(incluyeIva);
        return m;
    }

    /**
     * Navarra CPV viene tipo:
     * "60130000,Servicios ...|60130000,Servicios ...|"
     * o con '.' al final en la descripción.
     */
    private static List<Cpv> parseCpvs(String cpvRaw) {
        if (cpvRaw == null || cpvRaw.isBlank()) return Collections.emptyList();

        String[] parts = cpvRaw.split("\\|");
        LinkedHashMap<String, Cpv> dedup = new LinkedHashMap<>();

        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isBlank()) continue;

            String codePart = t;
            String descPart = null;

            int comma = t.indexOf(',');
            if (comma > 0) {
                codePart = t.substring(0, comma).trim();
                descPart = t.substring(comma + 1).trim();
            }

            // nos quedamos con dígitos (códigos CPV suelen ser 8)
            String code = codePart.replaceAll("\\D+", "");
            if (code.isBlank()) continue;

            // normaliza a 8 si viniera más largo por basura
            if (code.length() > 8) code = code.substring(0, 8);

            Cpv c = new Cpv();
            c.setCodigo(code);

            // si tu Cpv tiene campo nombre/descripcion, lo intentamos setear
            setIfPresentStatic(c, "setNombre", String.class, descPart);
            setIfPresentStatic(c, "setDescripcion", String.class, descPart);

            dedup.putIfAbsent(code, c);
        }

        List<Cpv> out = new ArrayList<>(dedup.values());
        if (!out.isEmpty()) out.get(0).setPrincipal(true);

        return out;
    }

    private static EstadoLicitacion mapEstado(String estadoTxt) {
        if (estadoTxt == null) return null;
        String e = estadoTxt.trim().toUpperCase(Locale.ROOT);

        // Navarra: PUBLICADO / CANCELADO
        if (e.contains("CANCEL")) {
            try { return EstadoLicitacion.CANCELADA; } catch (Exception ignore) {}
        }
        if (e.contains("PUBLIC")) {
            try { return EstadoLicitacion.PUBLICADA; } catch (Exception ignore) {}
        }

        // fallback si coincide exactamente con tu enum
        try { return EstadoLicitacion.valueOf(e); } catch (Exception ignore) {}
        return null;
    }

    private static TipoContrato mapTipoContrato(String tipoTxt) {
        if (tipoTxt == null) return null;
        String t = tipoTxt.trim().toUpperCase(Locale.ROOT);

        // Navarra: "Servicios", "Obras", "Suministro"
        if (t.startsWith("SERV")) return TipoContrato.SERVICIOS;
        if (t.startsWith("OBR")) return TipoContrato.OBRAS;
        if (t.startsWith("SUM")) return TipoContrato.SUMINISTROS; // "Suministro" singular

        try { return TipoContrato.valueOf(t); } catch (Exception ignore) {}
        return null;
    }

    /**
     * Setea un campo SOLO si existe el setter (para que compile aunque tu entidad aún no tenga esos campos).
     */
    private static void setIfPresent(Object target, String setterName, Class<?> paramType, Object value) {
        if (target == null || value == null) return;
        try {
            Method m = target.getClass().getMethod(setterName, paramType);
            m.invoke(target, value);
        } catch (Exception ignore) {
            // no existe o no accesible -> no pasa nada
        }
    }

    /** Variante estática para usarla desde métodos static (Cpv). */
    private static void setIfPresentStatic(Object target, String setterName, Class<?> paramType, Object value) {
        setIfPresent(target, setterName, paramType, value);
    }
}

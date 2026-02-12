package com.ordenatec.portallicitaciones.api.dto;

import com.ordenatec.portallicitaciones.infra.persistence.entity.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;

public class LicitacionFullDtoMapper {

    private LicitacionFullDtoMapper() {}

    public static LicitacionFullDto map(LicitacionEntity e) {

        LicitacionFullDto dto = new LicitacionFullDto();

        // ===========================
        // Core / tracking
        // ===========================
        dto.idDb = e.getId();
        dto.uuid = e.getUuid();
        dto.fechaUltimaActualizacion = e.getFechaUltimaActualizacion();

        dto.expediente = e.getExpediente();
        dto.titulo = e.getTitulo();

        // ===========================
        // URLs / RAW
        // ===========================
        dto.urlPublica = e.getUrlPublica();
        dto.urlDetalle = e.getUrlDetalle();
        dto.rawAtomJson = e.getRawAtomJson();
        dto.rawDetalleXml = e.getRawDetalleXml();

        // ===========================
        // Texto descriptivo
        // ===========================
        dto.entidad = e.getEntidad();
        dto.procedimiento = e.getProcedimiento();
        dto.tipoContratoTexto = e.getTipoContratoTexto();
        dto.modalidad = e.getModalidad();
        dto.criterioAdjudicacion = e.getCriterioAdjudicacion();
        dto.codigoNuts = e.getCodigoNuts();
        dto.lugarEjecucion = e.getLugarEjecucion();
        dto.estadoTexto = e.getEstadoTexto();
        dto.motivo = e.getMotivo();
        dto.plazoEjecucion = e.getPlazoEjecucion();

        // ===========================
        // Fechas / importes
        // ===========================
        dto.fechaPublicacion = e.getFechaPublicacion();
        dto.fechaLimitePresentacion = e.getFechaLimitePresentacion();
        dto.precioLicitacion = e.getPrecioLicitacion();
        dto.valorEstimado = e.getValorEstimado();
        dto.moneda = e.getMoneda();

        // ===========================
        // Organismo (tolerante)
        // ===========================
        if (e.getOrganismo() != null) {
            OrganismoEntity o = e.getOrganismo();

            Long id = safeGetLong(o, "getId");
            String nombre = safeGetString(o, "getNombre", "getDenominacion", "getRazonSocial");
            String nif = safeGetString(o, "getNif", "getCif");
            String codigo = safeGetString(o, "getCodigo", "getCodigoDir3", "getDir3");

            dto.organismo = new LicitacionFullDto.OrganismoDto(id, nombre, nif, codigo);
        }

        // ===========================
        // CPVs
        // ===========================
        if (e.getCpvs() != null) {
            dto.cpvs = e.getCpvs().stream()
                    .map(c -> safeGetString(c, "getCodigo", "getCpv", "getValor"))
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted()
                    .toList();
        }

        // ===========================
        // Documentos (tolerante)
        // ===========================
        if (e.getDocumentos() != null) {
            dto.documentos = e.getDocumentos().stream()
                    .sorted(Comparator.comparing(d -> {
                        Long id = safeGetLong(d, "getId");
                        return id == null ? Long.MAX_VALUE : id;
                    }))
                    .map(d -> new LicitacionFullDto.DocumentoDto(
                            safeGetLong(d, "getId"),
                            safeGetString(d, "getTipo", "getCategoria"),
                            safeGetString(d, "getTitulo", "getNombre", "getDescripcion"),
                            safeGetString(d, "getUrl", "getEnlace", "getHref")
                    ))
                    .toList();
        }

        // ===========================
        // Lotes (tolerante)
        // ===========================
        if (e.getLotes() != null) {
            dto.lotes = e.getLotes().stream()
                    .sorted(Comparator.comparing(l -> {
                        Long id = safeGetLong(l, "getId");
                        return id == null ? Long.MAX_VALUE : id;
                    }))
                    .map(l -> new LicitacionFullDto.LoteDto(
                            safeGetLong(l, "getId"),
                            safeGetString(l, "getTitulo", "getNombre", "getDescripcion"),
                            safeGetBigDecimal(l, "getImporte", "getPresupuesto", "getImporteSinIva", "getValor"),
                            safeGetString(l, "getMoneda", "getDivisa")
                    ))
                    .toList();
        }

        // ===========================
        // Adjudicación (tolerante)
        // ===========================
        if (e.getAdjudicacion() != null) {
            AdjudicacionEntity a = e.getAdjudicacion();

            Long id = safeGetLong(a, "getId");

            // aquí probamos varios nombres típicos
            String adjudicatario = safeGetString(a,
                    "getAdjudicatario",
                    "getEmpresa",
                    "getRazonSocial",
                    "getNombreEmpresa",
                    "getNombreAdjudicatario"
            );

            BigDecimal importe = safeGetBigDecimal(a,
                    "getImporte",
                    "getImporteAdjudicacion",
                    "getPrecio",
                    "getImporteSinIva",
                    "getValor"
            );

            String moneda = safeGetString(a, "getMoneda", "getDivisa");

            LocalDate fecha = safeGetLocalDate(a,
                    "getFecha",
                    "getFechaAdjudicacion",
                    "getFechaResolucion"
            );

            dto.adjudicacion = new LicitacionFullDto.AdjudicacionDto(id, adjudicatario, importe, moneda, fecha);
        }

        return dto;
    }

    // ======================================================
    // Helpers reflection (para tolerar nombres de getters)
    // ======================================================

    private static String safeGetString(Object target, String... methodNames) {
        Object v = safeInvoke(target, methodNames);
        return v == null ? null : String.valueOf(v);
    }

    private static Long safeGetLong(Object target, String... methodNames) {
        Object v = safeInvoke(target, methodNames);
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception ex) { return null; }
    }

    private static BigDecimal safeGetBigDecimal(Object target, String... methodNames) {
        Object v = safeInvoke(target, methodNames);
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception ex) { return null; }
    }

    private static LocalDate safeGetLocalDate(Object target, String... methodNames) {
        Object v = safeInvoke(target, methodNames);
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof java.sql.Date d) return d.toLocalDate();
        if (v instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return null;
    }

    private static Instant safeGetInstant(Object target, String... methodNames) {
        Object v = safeInvoke(target, methodNames);
        if (v == null) return null;
        if (v instanceof Instant i) return i;
        if (v instanceof java.util.Date d) return d.toInstant();
        return null;
    }

    private static Object safeInvoke(Object target, String... methodNames) {
        if (target == null || methodNames == null) return null;
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Exception ignored) {
                // probamos siguiente
            }
        }
        return null;
    }
}

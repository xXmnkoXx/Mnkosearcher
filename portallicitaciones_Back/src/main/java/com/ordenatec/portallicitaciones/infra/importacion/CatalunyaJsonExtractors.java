package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades para extraer campos "core" del JSON de Catalunya (analisi.transparenciacatalunya.cat)
 * de forma robusta (best-effort).
 *
 * Nota: El dataset viene "plano" (no hay un endpoint 1-a-1 tipo ATOM), pero incluye URLs
 * (enllac_publicacio, url_json_*) y muchos campos reutilizables para montar Licitacion + Lotes + CPVs + Adjudicacion.
 */
public final class CatalunyaJsonExtractors {

    private static final Pattern CPV_8_DIGITS = Pattern.compile("\\b(\\d{8})\\b");
    private static final DateTimeFormatter ISO_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]");

    private CatalunyaJsonExtractors() {}

    /* =========================
       CAMPOS BÁSICOS
       ========================= */

    public static String expediente(JsonNode row) {
        // en el dataset: codi_expedient
        return text(row, "codi_expedient");
    }

    public static String titulo(JsonNode row) {
        // denominacio suele ser el título visible; fallback a objecte_contracte
        String t = text(row, "denominacio");
        if (t == null) t = text(row, "objecte_contracte");
        return t;
    }

    public static String urlPublica(JsonNode row) {
        // enllac_publicacio: { "url": "https://contractaciopublica.cat/..." }
        return nestedUrl(row, "enllac_publicacio");
    }

    public static String urlJsonLicitacio(JsonNode row) {
        return nestedUrl(row, "url_json_licitacio");
    }

    public static String urlJsonAvaluacio(JsonNode row) {
        return nestedUrl(row, "url_json_avaluacio");
    }

    public static String urlJsonAdjudicacio(JsonNode row) {
        return nestedUrl(row, "url_json_adjudicacio");
    }

    public static String urlJsonFormalitzacio(JsonNode row) {
        return nestedUrl(row, "url_json_formalitzacio");
    }

    public static String fasePublicacio(JsonNode row) {
        // Ej: "Expedient en avaluació", "Adjudicació", "Formalització", ...
        return text(row, "fase_publicacio");
    }

    public static String procediment(JsonNode row) {
        return text(row, "procediment");
    }

    public static String tipusContracteRaw(JsonNode row) {
        return text(row, "tipus_contracte");
    }

    public static String tramitacio(JsonNode row) {
        // tipus_tramitacio
        return text(row, "tipus_tramitacio");
    }

    /* =========================
       FECHAS (PUBLICACION / LIMITE)
       ========================= */

    public static LocalDate fechaLimitePresentacion(JsonNode row) {
        // termini_presentacio_ofertes: "2025-09-29T14:00:00.000"
        return toLocalDate(text(row, "termini_presentacio_ofertes"));
    }

    /**
     * Fecha de publicación "más útil" para el feed diario:
     * - Si hay un campo data_publicacio_* acorde a la fase, se usa ese
     * - Si no, se coge la máxima fecha disponible entre los data_publicacio_* que existan
     */
    public static LocalDate fechaPublicacionBestEffort(JsonNode row) {
        String fase = safeLower(fasePublicacio(row));

        // Prioridad por fase
        if (fase != null) {
            if (fase.contains("anunci") || fase.contains("licit")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_anunci"));
                if (d != null) return d;
            }
            if (fase.contains("avalu")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_avaluacio"));
                if (d != null) return d;
            }
            if (fase.contains("adjud")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_adjudicacio"));
                if (d != null) return d;
            }
            if (fase.contains("formal")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_formalitzacio"));
                if (d != null) return d;
            }
            if (fase.contains("previ")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_previ"));
                if (d != null) return d;
            }
            if (fase.contains("futura")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_futura"));
                if (d != null) return d;
            }
            if (fase.contains("anul")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_anul"));
                if (d != null) return d;
            }
            if (fase.contains("consulta")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_consulta"));
                if (d != null) return d;
            }
            if (fase.contains("contracte") || fase.contains("encarrec")) {
                LocalDate d = toLocalDate(text(row, "data_publicacio_contracte"));
                if (d != null) return d;
                d = toLocalDate(text(row, "data_publicacio_encarrec"));
                if (d != null) return d;
            }
        }

        // Fallback: max fecha entre todas
        LocalDate max = null;
        max = max(max, toLocalDate(text(row, "data_publicacio_futura")));
        max = max(max, toLocalDate(text(row, "data_publicacio_previ")));
        max = max(max, toLocalDate(text(row, "data_publicacio_anunci")));
        max = max(max, toLocalDate(text(row, "data_publicacio_adjudicacio")));
        max = max(max, toLocalDate(text(row, "data_publicacio_formalitzacio")));
        max = max(max, toLocalDate(text(row, "data_publicacio_anul")));
        max = max(max, toLocalDate(text(row, "data_publicacio_consulta")));
        max = max(max, toLocalDate(text(row, "data_publicacio_avaluacio")));
        max = max(max, toLocalDate(text(row, "data_publicacio_contracte")));
        max = max(max, toLocalDate(text(row, "data_publicacio_encarrec")));
        return max;
    }

    /* =========================
       IMPORTES (best-effort)
       ========================= */

    /**
     * Presupuesto base (si existe):
     * - preferimos "pressupost_licitacio_amb" (incluye IVA)
     * - si no, "pressupost_licitacio_sense" (sin IVA)
     *
     * Ojo: hay variantes *_1 que parecen totales agregados; aquí devolvemos el "principal".
     */
    public static BigDecimal presupuestoBaseAmount(JsonNode row) {
        BigDecimal amb = toBigDecimal(text(row, "pressupost_licitacio_amb"));
        if (amb != null) return amb;

        BigDecimal sense = toBigDecimal(text(row, "pressupost_licitacio_sense"));
        if (sense != null) return sense;

        // fallback a valor_estimat_contracte
        return toBigDecimal(text(row, "valor_estimat_contracte"));
    }

    public static Boolean presupuestoIncluyeIVA(JsonNode row) {
        BigDecimal amb = toBigDecimal(text(row, "pressupost_licitacio_amb"));
        if (amb != null) return Boolean.TRUE;

        BigDecimal sense = toBigDecimal(text(row, "pressupost_licitacio_sense"));
        if (sense != null) return Boolean.FALSE;

        return null;
    }

    public static BigDecimal valorEstimadoAmount(JsonNode row) {
        // priorizamos valor_estimat_expedient (si viene) y si no valor_estimat_contracte
        BigDecimal ve = toBigDecimal(text(row, "valor_estimat_expedient"));
        if (ve != null) return ve;
        return toBigDecimal(text(row, "valor_estimat_contracte"));
    }

    /* =========================
       CPV
       ========================= */

    /**
     * Devuelve CPVs (8 dígitos) del campo codi_cpv.
     * Ejemplos:
     *  - "70130000-1" -> "70130000"
     *  - "79411000-8; 72200000-7" -> "79411000","72200000"
     */
    public static Set<String> extractCpvCodes(JsonNode row) {
        Set<String> out = new LinkedHashSet<>();
        String raw = text(row, "codi_cpv");
        if (raw == null) return out;

        // puede venir separado por ; , espacios o saltos
        String normalized = raw.replace('|', ';')
                .replace(',', ';')
                .replace('\n', ';')
                .replace('\r', ';');

        // metemos todo a regex (saca los 8 dígitos)
        Matcher m = CPV_8_DIGITS.matcher(normalized);
        while (m.find()) out.add(m.group(1));

        // si no pilló nada, intentamos trocear y limpiar "-X"
        if (out.isEmpty()) {
            for (String part : normalized.split(";")) {
                String p = part.trim();
                if (p.isEmpty()) continue;
                // "70130000-1" -> "70130000"
                int dash = p.indexOf('-');
                if (dash > 0) p = p.substring(0, dash).trim();
                if (p.matches("\\d{8}")) out.add(p);
            }
        }
        return out;
    }

    /* =========================
       LOTE (dataset viene ya por lote)
       ========================= */

    public static String numeroLote(JsonNode row) {
        return text(row, "numero_lot");
    }

    public static String descripcionLote(JsonNode row) {
        return text(row, "descripcio_lot");
    }

    /* =========================
       ADJUDICACION (best-effort)
       ========================= */

    public static String adjudicatarioNombre(JsonNode row) {
        return text(row, "denominacio_adjudicatari");
    }

    public static String adjudicatarioNif(JsonNode row) {
        return text(row, "identificacio_adjudicatari");
    }

    public static BigDecimal importeAdjudicacionAmbIva(JsonNode row) {
        return toBigDecimal(text(row, "import_adjudicacio_amb_iva"));
    }

    public static BigDecimal importeAdjudicacionSenseIva(JsonNode row) {
        return toBigDecimal(text(row, "import_adjudicacio_sense"));
    }

    public static Integer ofertesRebudes(JsonNode row) {
        String s = text(row, "ofertes_rebudes");
        if (s == null) return null;
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    public static String resultat(JsonNode row) {
        // Ej: "Adjudicació", "Formalització"
        return text(row, "resultat");
    }

    /* =========================
       ORGANISMO (para tu tabla OrganismoEntity)
       ========================= */

    public static String organismoNombre(JsonNode row) {
        // nom_organ
        return text(row, "nom_organ");
    }

    public static String organismoDir3(JsonNode row) {
        // codi_dir3
        return text(row, "codi_dir3");
    }

    public static String organismoNif(JsonNode row) {
        // En el dataset no siempre viene NIF explícito; a veces en identificacio_adjudicatari (que NO es el organismo).
        // Aquí devolvemos null para no meter basura.
        return null;
    }

    public static String organismoTipoAdministracion(JsonNode row) {
        // nom_ambit: "Departaments..." / "Entitats..."
        return text(row, "nom_ambit");
    }

    /* =========================
       UTILIDADES
       ========================= */

    public static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;

        // Socrata a veces devuelve objetos para links: {url: "..."} -> aquí no
        if (v.isObject()) return null;

        String s = v.asText();
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String nestedUrl(JsonNode node, String fieldObject) {
        if (node == null) return null;
        JsonNode obj = node.get(fieldObject);
        if (obj == null || obj.isNull() || !obj.isObject()) return null;
        JsonNode u = obj.get("url");
        if (u == null || u.isNull()) return null;
        String s = u.asText();
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static LocalDate toLocalDate(String maybeIsoDateTime) {
        if (maybeIsoDateTime == null) return null;

        // 1) si viene tipo "2025-12-24"
        if (maybeIsoDateTime.length() == 10 && maybeIsoDateTime.charAt(4) == '-' && maybeIsoDateTime.charAt(7) == '-') {
            try { return LocalDate.parse(maybeIsoDateTime); } catch (Exception ignored) {}
        }

        // 2) si viene tipo "2025-12-24T09:00:00.000"
        try {
            // sin zona: parse como LocalDateTime “flexible” y nos quedamos con la fecha
            return java.time.LocalDateTime.parse(maybeIsoDateTime, ISO_WITH_MILLIS).toLocalDate();
        } catch (Exception ignored) { }

        // 3) si viniera con offset (raro aquí, pero por robustez)
        try {
            return OffsetDateTime.parse(maybeIsoDateTime).toLocalDate();
        } catch (Exception ignored) { }

        return null;
    }

    private static BigDecimal toBigDecimal(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;

        // normalizar coma decimal si apareciera
        t = t.replace(",", ".");
        try {
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static String safeLower(String s) {
        return (s == null) ? null : s.toLowerCase(Locale.ROOT);
    }
}

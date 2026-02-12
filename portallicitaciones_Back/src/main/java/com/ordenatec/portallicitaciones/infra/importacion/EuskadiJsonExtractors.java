package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extractores defensivos para Euskadi.
 *
 * NOTA:
 * - Esta API a veces devuelve fechas en "YYYY-MM-DD" y otras en ISO con hora ("YYYY-MM-DDTHH:mm:ssZ").
 * - Los CPV pueden venir como array de objetos o como strings (según endpoint).
 */
public final class EuskadiJsonExtractors {

    private EuskadiJsonExtractors() {}

    /* =========================
       HELPERS GENERALES
       ========================= */

    public static String text(JsonNode n, String field) {
        if (n == null || field == null) return null;
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText(null);
        return (s == null || s.isBlank()) ? null : s;
    }

    public static String nestedText(JsonNode n, String... path) {
        if (n == null || path == null || path.length == 0) return null;
        JsonNode cur = n;
        for (String p : path) {
            if (cur == null || cur.isNull()) return null;
            cur = cur.path(p);
        }
        if (cur == null || cur.isNull()) return null;
        String s = cur.asText(null);
        return (s == null || s.isBlank()) ? null : s;
    }

    /* =========================
       CAMPOS PRINCIPALES
       ========================= */

    /**
     * Título / objeto. Puede venir como:
     * - "title"
     * - "object"
     * - "description"
     * - "contractObject"
     */
    public static String titulo(JsonNode notice) {
        String t = text(notice, "title");
        if (t != null) return t;

        t = text(notice, "object");
        if (t != null) return t;

        t = text(notice, "contractObject");
        if (t != null) return t;

        t = text(notice, "description");
        return t;
    }

    /**
     * Código/expediente.
     * Suele venir en "code".
     */
    public static String code(JsonNode notice) {
        String c = text(notice, "code");
        if (c != null) return c;

        // fallback
        c = text(notice, "expedientCode");
        if (c != null) return c;

        return text(notice, "id");
    }

    /**
     * URL pública del anuncio (si viene).
     */
    public static String mainEntityOfPage(JsonNode notice) {
        String u = text(notice, "mainEntityOfPage");
        if (u != null) return u;
        return text(notice, "link");
    }

    /* =========================
       FECHAS
       ========================= */

    /**
     * firstPublicationDate (LocalDate):
     * - "firstPublicationDate" (ISO con hora o sin)
     * - o "publicationDate"
     */
    public static LocalDate firstPublicationDate(JsonNode n) {
        LocalDate d = parseLocalDateSafe(text(n, "firstPublicationDate"));
        if (d != null) return d;

        d = parseLocalDateSafe(text(n, "publicationDate"));
        if (d != null) return d;

        // fallback
        return parseLocalDateSafe(text(n, "firstPublication"));
    }

    /**
     * lastPublicationDate (LocalDate):
     * - "lastPublicationDate" (ISO con hora o sin)
     */
    public static LocalDate lastPublicationDate(JsonNode n) {
        LocalDate d = parseLocalDateSafe(text(n, "lastPublicationDate"));
        if (d != null) return d;

        // fallback
        return parseLocalDateSafe(text(n, "lastPublication"));
    }

    /**
     * deadlineDate (LocalDate):
     * - "deadlineDate"
     * - "tenderSubmissionDeadline"
     * - "submissionDeadline"
     * - o en nested: "tenderSubmissionDeadline.date"
     */
    public static LocalDate deadlineDate(JsonNode n) {
        LocalDate d = parseLocalDateSafe(text(n, "deadlineDate"));
        if (d != null) return d;

        d = parseLocalDateSafe(text(n, "tenderSubmissionDeadline"));
        if (d != null) return d;

        d = parseLocalDateSafe(text(n, "submissionDeadline"));
        if (d != null) return d;

        // nested
        d = parseLocalDateSafe(nestedText(n, "tenderSubmissionDeadline", "date"));
        if (d != null) return d;

        return parseLocalDateSafe(nestedText(n, "deadline", "date"));
    }

    /**
     * Fecha como Instant (si te hiciera falta en algún lado).
     */
    public static Instant parseInstantSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // si viene con Z / offset
            return OffsetDateTime.parse(s).toInstant();
        } catch (DateTimeParseException ignored) {
        }
        try {
            // si viene solo YYYY-MM-DD -> lo tratamos como inicio de día UTC (instant aproximado)
            LocalDate d = LocalDate.parse(s.substring(0, 10));
            return d.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Parse robusto a LocalDate soportando:
     * - "YYYY-MM-DD"
     * - "YYYY-MM-DDTHH:mm:ssZ"
     * - "YYYY-MM-DDTHH:mm:ss+01:00"
     */
    public static LocalDate parseLocalDateSafe(String s) {
        if (s == null || s.isBlank()) return null;

        String v = s.trim();
        try {
            // si es justo yyyy-MM-dd
            if (v.length() >= 10) {
                return LocalDate.parse(v.substring(0, 10));
            }
        } catch (Exception ignored) {
        }

        // fallback: intenta parsear como offset datetime
        try {
            return OffsetDateTime.parse(v).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }

    /* =========================
       CPV
       ========================= */

    /**
     * Extrae CPVs de distintas estructuras posibles:
     *
     * Caso A:
     *  "cpv": [{"code":"12345678"}, {"code":"87654321"}]
     *
     * Caso B:
     *  "cpvCodes": ["12345678","87654321"]
     *
     * Caso C:
     *  "cpv": ["12345678","87654321"]
     *
     * Caso D (nested):
     *  "contract": {"cpv": [...]}
     */
    public static Set<String> extractCpvCodes(JsonNode n) {
        Set<String> out = new LinkedHashSet<>();
        if (n == null || n.isNull()) return out;

        // 1) cpv (array)
        JsonNode cpv = n.path("cpv");
        readCpvArray(out, cpv);

        // 2) cpvCodes (array)
        JsonNode cpvCodes = n.path("cpvCodes");
        readCpvArray(out, cpvCodes);

        // 3) nested contract.cpv
        JsonNode contractCpv = n.path("contract").path("cpv");
        readCpvArray(out, contractCpv);

        // 4) nested mainCpv / additionalCpvs
        JsonNode mainCpv = n.path("mainCpv");
        readCpvArray(out, mainCpv);

        JsonNode addCpvs = n.path("additionalCpvs");
        readCpvArray(out, addCpvs);

        return out;
    }

    private static void readCpvArray(Set<String> out, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return;

        if (node.isArray()) {
            for (JsonNode it : node) {
                if (it == null || it.isNull()) continue;

                // objeto: { "code": "12345678" }
                String code = text(it, "code");
                if (code == null) {
                    // string directo: "12345678"
                    code = it.asText(null);
                }

                code = normalizeCpv(code);
                if (code != null) out.add(code);
            }
            return;
        }

        // si no es array pero es objeto único
        if (node.isObject()) {
            String code = text(node, "code");
            code = normalizeCpv(code);
            if (code != null) out.add(code);
            return;
        }

        // si es string suelto
        String code = node.asText(null);
        code = normalizeCpv(code);
        if (code != null) out.add(code);
    }

    private static String normalizeCpv(String code) {
        if (code == null) return null;
        String c = code.trim();
        if (c.isBlank()) return null;

        // CPV suele ser 8 dígitos, a veces trae guiones o espacios
        c = c.replace("-", "").replace(" ", "");

        // si trae más cosas, recortamos a 8 si empieza por dígitos
        if (c.length() > 8) {
            String maybe = c.substring(0, 8);
            if (maybe.chars().allMatch(Character::isDigit)) return maybe;
        }

        return c;
    }
}

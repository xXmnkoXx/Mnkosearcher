package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import java.util.*;

/**
 * Esquema XLSX compatible OpenPLACSP 2.2
 *
 * Define:
 * - nombres de hojas
 * - cabeceras oficiales
 * - mapeo normalizado clave -> header
 *
 * Esta clase NO escribe Excel.
 * Solo define estructura.
 */
public final class OpenPlacspXlsxSchema {

    private OpenPlacspXlsxSchema() {}

    // =========================
    // HOJAS
    // =========================

    public static final String SHEET_LICITACIONES = "Licitaciones";
    public static final String SHEET_ENCARGOS = "Encargos a medios propios";
    public static final String SHEET_CONSULTAS = "Consultas Preliminares";

    // =========================
    // LICITACIONES (OFICIAL)
    // =========================

    public static final List<String> LICITACIONES_HEADERS = List.of(
            "Identificador",
            "Link licitación",
            "Fecha actualización",
            "Vigente/Anulada/Archivada",
            "Primera publicación",
            "Estado",
            "Número de expediente",
            "Objeto del Contrato",
            "Identificador único TED",
            "Valor estimado del contrato",
            "Presupuesto base sin impuestos",
            "Presupuesto base con impuestos",
            "CPV",
            "Tipo de contrato",
            "Contrato mixto",
            "Lugar de ejecución",
            "Órgano de Contratación",
            "ID OC en PLACSP",
            "NIF OC",
            "DIR3",
            "Enlace al Perfil de Contratante del OC",
            "Tipo de Administración",
            "Código Postal",
            "Tipo de procedimiento",
            "Sistema de contratación",
            "Tramitación",
            "Forma de presentación de la oferta",
            "Fecha de presentación de ofertas",
            "Fecha de presentación de solicitudes de participacion",
            "Directiva de aplicación",
            "Contrato SARA/Umbral",
            "Financiación Europea y fuente",
            "Descripción de la financiación europea",
            "Subasta electrónica",
            "Subcontratación permitida",
            "Subcontratación permitida porcentaje",
            "Número de expediente", // DUPLICADO REAL EN OPENPLACSP
            "Lote",
            "Objeto licitación/lote",
            "Valor estimado licitación/lote",
            "Presupuesto base con impuestos licitación/lote",
            "Presupuesto base sin impuestos licitación/lote",
            "CPV licitación/lote",
            "Lugar ejecución licitación/lote",
            "Resultado licitación/lote",
            "Fecha del acuerdo licitación/lote",
            "Número de ofertas recibidas por licitación/lote",
            "Precio de la oferta más baja por licitación/lote",
            "Precio de la oferta más alta por licitación/lote",
            "Se han excluído ofertas por ser anormalmente bajas por licitación/lote",
            "Número del contrato licitación/lote",
            "Fecha formalización del contrato licitación/lote",
            "Fecha entrada en vigor del contrato de licitación/lote",
            "Adjudicatario licitación/lote",
            "Tipo de identificador de adjudicatario por licitación/lote",
            "Identificador Adjudicatario de la licitación/lote",
            "El adjudicatario es o no PYME de la licitación/lote",
            "Importe adjudicación sin impuestos licitación/lote",
            "Importe adjudicación con impuestos licitación/lote"
    );

    // =========================
    // ENCARGOS A MEDIOS PROPIOS
    // =========================

    public static final List<String> ENCARGOS_HEADERS = List.of(
            "Identificador",
            "Link Encargo",
            "Fecha actualización",
            "Vigente/Anulada/Archivada",
            "Primera publicación",
            "Estado",
            "Número de expediente",
            "Objeto del Encargo",
            "Presupuesto base sin impuestos",
            "Presupuesto base con impuestos",
            "CPV",
            "Tipo de encargo",
            "Lugar de ejecución",
            "Órgano de Contratación",
            "ID OC en PLACSP",
            "NIF OC",
            "DIR3",
            "Enlace al Perfil de Contratante del OC",
            "Tipo de Administración",
            "Código Postal",
            "Fecha del acuerdo del encargo",
            "Medio propio personificado",
            "NIF Medio propio personificado",
            "ID_PLATAFORMA Medio propio personificado"
    );

    // =========================
    // CONSULTAS PRELIMINARES
    // =========================

    public static final List<String> CONSULTAS_HEADERS = List.of(
            "Identificador",
            "Link Consulta",
            "Fecha actualización",
            "Vigente/Anulada/Archivada",
            "Primera publicación",
            "Estado",
            "Número de consulta preliminar",
            "Objeto de la consulta",
            "Fecha de incio de la consulta",
            "Fecha límite de respuesta",
            "Dirección para presentación",
            "Tipo de consulta",
            "Condiciones o términos de envío de la consulta",
            "Futura licitación. Tipo de contrato",
            "Futura licitación. Objeto",
            "Futura licitación. Procedimiento",
            "CPV",
            "Órgano de Contratación",
            "ID OC en PLACSP",
            "NIF OC",
            "DIR3",
            "Enlace al Perfil de Contratante del OC",
            "Tipo de Administración",
            "Código Postal"
    );

    // =========================
    // FACTORY DEFAULT
    // =========================

    public static OpenPlacspXlsxSchema defaultSchema() {
        return new OpenPlacspXlsxSchema();
    }

    // =========================
    // API PÚBLICA
    // =========================

    public List<String> headers() {
        return LICITACIONES_HEADERS;
    }

    /**
     * Convierte el mapa plano extraído del ATOM/UBL
     * al mapa final XLSX (header -> value)
     *
     * De momento passthrough.
     * Más adelante podemos mapear campos finos aquí.
     */
    public Map<String, String> toRowMap(Map<String, String> raw) {
        if (raw == null) return Map.of();

        Map<String, String> out = new LinkedHashMap<>();

        // Escribimos SOLO columnas oficiales
        for (String h : LICITACIONES_HEADERS) {

            // normalización clave simple
            String k = normalize(h);

            String v = raw.get(k);

            if (v != null) {
                out.put(h, v);
            }
        }

        return out;
    }

    // =========================
    // NORMALIZACIÓN
    // =========================

    private static String normalize(String s) {
        if (s == null) return null;

        return s.toLowerCase(Locale.ROOT)
                .replace("á","a")
                .replace("é","e")
                .replace("í","i")
                .replace("ó","o")
                .replace("ú","u")
                .replace("ñ","n")
                .replace("ü","u")
                .replace("/", "_")
                .replace(".", "")
                .replace("(", "")
                .replace(")", "")
                .replace(" ", "_")
                .replace("__", "_")
                .trim();
    }
}

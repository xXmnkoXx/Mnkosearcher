package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cliente de "enriquecimiento" para Euskadi:
 * - Dado un href de contracts, descarga el JSON y lo devuelve
 * - Dado un id de notice, descarga el detalle del notice y lo devuelve
 *
 * IMPORTANTE:
 * - La API devuelve muchas veces hrefs RELATIVOS ("/procurements/...").
 *   Este cliente los convierte a absolutos siempre.
 */
@Component
public class EuskadiEnrichmentClient {

    private static final Logger log = LoggerFactory.getLogger(EuskadiEnrichmentClient.class);

    private static final String EUSKADI_BASE = "https://api.euskadi.eus";
    private static final String NOTICE_DETAIL_PATH = "/procurements/contracting-notices/";

    private final EuskadiApiClient apiClient;

    public EuskadiEnrichmentClient(EuskadiApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Descarga contracts a partir del href devuelto por la API en:
     *   item._links.contracts.href
     *
     * El href puede ser:
     *  - absoluto: https://api.euskadi.eus/...
     *  - relativo: /procurements/...
     */
    public JsonNode fetchContractsByHref(String href) {
        if (href == null || href.isBlank()) return null;

        String url = normalizeUrl(href);

        try {
            return apiClient.getJson(url);
        } catch (Exception ex) {
            log.warn("[EUSKADI] fetchContractsByHref fallo. href={} url={} err={}", href, url, ex.toString());
            throw ex;
        }
    }

    /**
     * Compatibilidad: si ya tenías este método, lo dejamos.
     * Construye URL absoluta a partir del id.
     */
    public JsonNode fetchNoticeDetail(long id) {
        return fetchNoticeDetailAbsolute(id);
    }

    /**
     * ✅ Método NUEVO (el que te faltaba) para evitar problemas con URLs relativas.
     * Siempre construye URL absoluta:
     *   https://api.euskadi.eus/procurements/contracting-notices/{id}
     */
    public JsonNode fetchNoticeDetailAbsolute(long id) {
        if (id <= 0) return null;

        String url = EUSKADI_BASE + NOTICE_DETAIL_PATH + id;

        try {
            return apiClient.getJson(url);
        } catch (Exception ex) {
            log.warn("[EUSKADI] fetchNoticeDetailAbsolute fallo. id={} url={} err={}", id, url, ex.toString());
            throw ex;
        }
    }

    /**
     * Convierte href relativo -> absoluto.
     * - "/procurements/..." -> "https://api.euskadi.eus/procurements/..."
     * - "procurements/..."  -> "https://api.euskadi.eus/procurements/..."
     */
    private String normalizeUrl(String href) {
        String h = href.trim();

        if (h.startsWith("http://") || h.startsWith("https://")) {
            return h;
        }

        if (!h.startsWith("/")) {
            h = "/" + h;
        }

        return EUSKADI_BASE + h;
    }
}

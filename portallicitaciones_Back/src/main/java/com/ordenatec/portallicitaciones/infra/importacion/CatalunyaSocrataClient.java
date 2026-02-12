package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente para el dataset Socrata (SODA) de Cataluña:
 * https://analisi.transparenciacatalunya.cat/resource/ybgg-dgi6.json
 *
 * NOTA: por tu requisito, este cliente NO filtra "ayer/hoy" en la API.
 * Solo pagina ($limit/$offset) y devuelve el JSON para que filtres en código.
 */
@Component
public class CatalunyaSocrataClient {

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public CatalunyaSocrataClient(
            ObjectMapper mapper,
            @Value("${catalunya.api.base-url:https://analisi.transparenciacatalunya.cat}") String baseUrl
    ) {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        this.mapper = mapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Devuelve una página del dataset ybgg-dgi6.
     *
     * @param limit  tamaño de página (recomendado 5000-50000 según aguante)
     * @param offset offset de paginación
     */
    public JsonNode fetchPage(int limit, int offset) {
        if (limit <= 0) throw new IllegalArgumentException("limit debe ser > 0");
        if (offset < 0) throw new IllegalArgumentException("offset debe ser >= 0");

        String url = baseUrl + "/resource/ybgg-dgi6.json"
                + "?$limit=" + limit
                + "&$offset=" + offset;

        return getJson(url);
    }

    /**
     * Variante opcional por si luego quieres pasar un $where/$order.
     * (No la usamos ahora, pero te deja el client preparado.)
     */
    public JsonNode fetchPage(int limit, int offset, String where, String orderBy) {
        if (limit <= 0) throw new IllegalArgumentException("limit debe ser > 0");
        if (offset < 0) throw new IllegalArgumentException("offset debe ser >= 0");

        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl).append("/resource/ybgg-dgi6.json")
          .append("?$limit=").append(limit)
          .append("&$offset=").append(offset);

        if (where != null && !where.isBlank()) {
            sb.append("&$where=").append(urlEncode(where));
        }
        if (orderBy != null && !orderBy.isBlank()) {
            sb.append("&$order=").append(urlEncode(orderBy));
        }

        return getJson(sb.toString());
    }

    // ---------------- internals ----------------

    private JsonNode getJson(String url) {
        int maxRetries = 5;
        long backoffMs = 800;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(60))
                        .header("User-Agent", "OrdenatecPortalLicitaciones/1.0")
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();

                // Rate limit / server errors -> retry con backoff
                if (status == 429 || (status >= 500 && status <= 599)) {
                    if (attempt == maxRetries) {
                        throw new IOException("HTTP " + status + " GET " + url);
                    }
                    sleep(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 8000);
                    continue;
                }

                if (status < 200 || status >= 300) {
                    throw new IOException("HTTP " + status + " GET " + url + " body=" + safeBody(resp.body()));
                }

                return mapper.readTree(resp.body());

            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();

                if (attempt == maxRetries) {
                    throw new RuntimeException("Error GET " + url, e);
                }

                sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, 8000);
            }
        }

        throw new RuntimeException("Error GET " + url);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrumpido durante backoff", ie);
        }
    }

    private static String safeBody(String body) {
        if (body == null) return "";
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

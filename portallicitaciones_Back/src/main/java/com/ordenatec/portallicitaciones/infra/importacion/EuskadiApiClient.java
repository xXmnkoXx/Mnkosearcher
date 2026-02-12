package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;

/**
 * Cliente genérico Euskadi (JSON).
 * - Soporta portal.http.insecureSsl=true para DEV (NO usar en producción)
 */
@Component
public class EuskadiApiClient {

    private static final Logger log = LoggerFactory.getLogger(EuskadiApiClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int timeoutMs;

    public EuskadiApiClient(
            ObjectMapper objectMapper,
            @Value("${portal.http.insecureSsl:false}") boolean insecureSsl,
            @Value("${portal.http.timeoutMs:30000}") int timeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.timeoutMs = timeoutMs;

        try {
            HttpClient.Builder b = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .followRedirects(HttpClient.Redirect.NORMAL);

            if (insecureSsl) {
                // DEV ONLY: confiar en cualquier certificado
                SSLContext ctx = insecureSslContext();
                b.sslContext(ctx);

                SSLParameters p = new SSLParameters();
                // No desactiva verificación de hostname en todos los casos,
                // pero para la mayoría de endpoints suele bastar junto al trustAll.
                // Si tu JDK/entorno sigue siendo estricto, lo correcto es arreglar truststore.
                b.sslParameters(p);

                log.warn("[HTTP] insecureSsl=true (NO USAR EN PRODUCCIÓN).");
            }

            this.httpClient = b.build();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo inicializar EuskadiApiClient", e);
        }
    }

    public JsonNode getJson(String baseUrl, Map<String, Object> queryParams) {
        String url = buildUrl(baseUrl, queryParams);
        return getJson(url);
    }

    public JsonNode getJson(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(safeUri(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", userAgent())
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = resp.statusCode();
            String body = resp.body();

            if (code < 200 || code >= 300) {
                String snippet = snippet(body, 350);
                throw new RuntimeException("HTTP " + code + " url=" + url + " body=" + snippet);
            }

            if (body == null || body.isBlank()) {
                return objectMapper.createObjectNode();
            }

            return objectMapper.readTree(body);

        } catch (Exception e) {
            throw new RuntimeException("EuskadiApiClient error al llamar/parsing " + url + " -> " + e.getMessage(), e);
        }
    }

    // ------------------ helpers ------------------

    private static String buildUrl(String base, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return base;

        StringBuilder sb = new StringBuilder(base);
        sb.append(base.contains("?") ? "&" : "?");

        boolean first = true;
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (e.getKey() == null) continue;
            Object v = e.getValue();
            if (v == null) continue;

            if (!first) sb.append("&");
            first = false;

            sb.append(encode(e.getKey())).append("=").append(encode(String.valueOf(v)));
        }
        return sb.toString();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static URI safeUri(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException ex) {
            return URI.create(url.replace(" ", "%20"));
        }
    }

    private static String userAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }

    private static String snippet(String s, int max) {
        if (s == null) return null;
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : (t.substring(0, max) + "...");
    }

    private static SSLContext insecureSslContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                    public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new SecureRandom());
        return ctx;
    }
}

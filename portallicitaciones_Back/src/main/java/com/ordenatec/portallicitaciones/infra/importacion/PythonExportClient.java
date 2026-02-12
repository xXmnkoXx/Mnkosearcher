package com.ordenatec.portallicitaciones.infra.importacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PythonExportClient {

    private final HttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;

    public PythonExportClient(
            @Value("${python.export.base-url}") String baseUrl,
            @Value("${python.export.api-key}") String apiKey,
            @Value("${python.export.timeout-seconds:600}") int timeoutSeconds
    ) {
        this.baseUrl = trimRightSlash(baseUrl);
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;

        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public InputStream openNdjsonStream(String source, Map<String, String> params) {
        String path = switch (source) {
            case "nacional" -> "/export/nacional";
            case "catalunya" -> "/export/catalunya";
            default -> throw new IllegalArgumentException("source inválido: " + source);
        };

        String qs = toQueryString(params);
        URI uri = URI.create(baseUrl + path + (qs.isEmpty() ? "" : "?" + qs));

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("X-API-KEY", apiKey)
                    .header("Accept", "application/x-ndjson,application/json;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<InputStream> resp =
                    client.send(req, HttpResponse.BodyHandlers.ofInputStream());

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                String body = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("Python export HTTP " + resp.statusCode() + " uri=" + uri + " body=" + snippet(body, 300));
            }

            return resp.body();

        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Python export: " + e.getMessage(), e);
        }
    }

    private static String toQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        return params.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && !e.getValue().isBlank())
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String trimRightSlash(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private static String snippet(String s, int max) {
        if (s == null) return null;
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}

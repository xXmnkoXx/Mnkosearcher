package com.ordenatec.portallicitaciones.infra.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Component
public class DetalleHttpClient {

    private static final Logger log = LoggerFactory.getLogger(DetalleHttpClient.class);

    public record FetchResult(
            int status,
            String finalUrl,
            String contentType,
            String body,
            String error
    ) {
        public boolean ok() { return status >= 200 && status < 300; }
    }

    private final HttpClient client;

    public DetalleHttpClient(
            @Value("${portal.http.insecureSsl:false}") boolean insecureSsl
    ) {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(20))
                .sslContext(buildSslContext(insecureSsl))
                .build();

        if (insecureSsl) {
            log.warn("[HTTP] insecureSsl=true (NO USAR EN PRODUCCIÓN).");
        } else {
            log.info("[HTTP] SSLContext = sistema (Windows-ROOT si está disponible).");
        }
    }

    public FetchResult get(String url) {
        String original = trimToNull(url);
        if (original == null) {
            return new FetchResult(0, null, null, null, "url is null/blank");
        }

        URI uri;
        try {
            uri = safeUri(original);
        } catch (Exception e) {
            return new FetchResult(0, original, null, null, "bad uri: " + e.getMessage());
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(35))
                    .header("User-Agent", userAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ca-ES,ca;q=0.9,es-ES,es;q=0.8,en;q=0.6")
                    .GET()
                    .build();

            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

            String contentType = resp.headers().firstValue("content-type").orElse(null);
            String finalUrl = resp.uri() != null ? resp.uri().toString() : original;

            String body = readBody(resp.body(), charsetFromContentType(contentType));

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                String msg = "http status " + resp.statusCode();
                if (body != null && !body.isBlank()) msg += " bodySnippet=" + snippet(body, 200);
                return new FetchResult(resp.statusCode(), finalUrl, contentType, body, msg);
            }

            return new FetchResult(resp.statusCode(), finalUrl, contentType, body, null);

        } catch (Exception e) {
            log.warn("[HTTP] ERROR url={} msg={}", original, e.toString());
            return new FetchResult(0, original, null, null, e.toString());
        }
    }

    // ---------------- SSL ----------------

    private static SSLContext buildSslContext(boolean insecureSsl) {
        try {
            if (insecureSsl) return insecureTrustAll();

            // 1) Intento: usar Windows-ROOT (en Windows con SunMSCAPI)
            try {
                KeyStore ks = KeyStore.getInstance("Windows-ROOT");
                ks.load(null, null);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);

                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
                return ctx;
            } catch (Exception e) {
                // 2) Fallback: default JVM truststore
                log.warn("[HTTP] No se pudo cargar Windows-ROOT. Usando truststore por defecto. Motivo={}", e.toString());
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, new SecureRandom());
                return ctx;
            }

        } catch (Exception e) {
            // último fallback: default
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, new SecureRandom());
                return ctx;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static SSLContext insecureTrustAll() throws Exception {
        TrustManager[] trustAll = new TrustManager[] {
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

    // -------------- utils --------------

    private static URI safeUri(String url) {
        try { return URI.create(url); }
        catch (IllegalArgumentException ex) { return URI.create(url.replace(" ", "%20")); }
    }

    private static String readBody(InputStream in, Charset cs) {
        if (in == null) return null;
        Charset charset = cs != null ? cs : StandardCharsets.UTF_8;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, charset))) {
            StringBuilder sb = new StringBuilder(32_000);
            char[] buf = new char[8192];
            int n;
            while ((n = br.read(buf)) >= 0) {
                sb.append(buf, 0, n);
                if (sb.length() > 2_000_000) break;
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static Charset charsetFromContentType(String contentType) {
        if (contentType == null) return StandardCharsets.UTF_8;
        String ct = contentType.toLowerCase();
        int idx = ct.indexOf("charset=");
        if (idx < 0) return StandardCharsets.UTF_8;

        String cs = ct.substring(idx + "charset=".length()).trim();
        int semi = cs.indexOf(';');
        if (semi >= 0) cs = cs.substring(0, semi).trim();
        cs = cs.replace("\"", "").trim();

        if (cs.isBlank()) return StandardCharsets.UTF_8;
        try { return Charset.forName(cs); }
        catch (Exception ignore) { return StandardCharsets.UTF_8; }
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

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
 
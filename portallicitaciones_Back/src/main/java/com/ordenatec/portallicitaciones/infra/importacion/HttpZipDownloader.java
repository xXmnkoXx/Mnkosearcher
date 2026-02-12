package com.ordenatec.portallicitaciones.infra.importacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLParameters;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpZipDownloader {

    private static final Logger log = LoggerFactory.getLogger(HttpZipDownloader.class);

    private final HttpClient client;

    public HttpZipDownloader() {
        // ✅ Fuerza TLS 1.2
        SSLParameters sslParams = new SSLParameters();
        sslParams.setProtocols(new String[]{"TLSv1.2"});

        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)     // ✅ evita problemas con HTTP/2
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .sslParameters(sslParams)
                .build();
    }

    public InputStream descargar(String url, DownloadProgressListener listener) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("URL vacía");
        return descargar(URI.create(url.trim()), listener);
    }

    /**
     * Descarga en streaming y devuelve InputStream (ideal para ZipInputStream).
     * El progreso se reporta cada ~2MB.
     */
    public InputStream descargar(URI uri, DownloadProgressListener listener) {
        if (uri == null) throw new IllegalArgumentException("URI nula");

        int maxRetries = 5;
        long backoffMs = 1_000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Descargando ZIP (intento {}/{}): {}", attempt, maxRetries, uri);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofMinutes(15)) // ZIP grande
                        .header("User-Agent", "Mozilla/5.0 OrdenatecPortalLicitaciones/1.0")
                        .header("Accept", "application/zip, application/octet-stream, */*")
                        .header("Accept-Encoding", "identity") // ✅ para Content-Length “real” si lo dan
                        .GET()
                        .build();

                HttpResponse<InputStream> resp =
                        client.send(req, HttpResponse.BodyHandlers.ofInputStream());

                int code = resp.statusCode();
                if (code < 200 || code >= 300) {
                    try (InputStream is = resp.body()) { /* consume & close */ }
                    throw new IOException("HTTP " + code + " descargando ZIP: " + uri);
                }

                long total = resp.headers()
                        .firstValueAsLong("Content-Length")
                        .orElse(-1);

                InputStream raw = resp.body();
                return new ProgressInputStream(raw, total, listener);

            } catch (IOException | InterruptedException e) {
                boolean last = (attempt == maxRetries);
                log.warn("Fallo descargando ZIP (intento {}/{}): {}", attempt, maxRetries, e.toString());

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                if (last) {
                    throw new RuntimeException("Error descargando ZIP: " + uri, e);
                }

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrumpido descargando ZIP: " + uri, ie);
                }
                backoffMs = Math.min(backoffMs * 2, 8_000);
            }
        }

        throw new RuntimeException("Error descargando ZIP: " + uri);
    }

    // ----------------- Progreso -----------------

    public interface DownloadProgressListener {
        void onProgress(long downloadedBytes, long totalBytes);
    }

    private static class ProgressInputStream extends FilterInputStream {
        private final long totalBytes;
        private final DownloadProgressListener listener;
        private long downloaded = 0;
        private long lastReported = 0;

        protected ProgressInputStream(InputStream in, long totalBytes, DownloadProgressListener listener) {
            super(in);
            this.totalBytes = totalBytes;
            this.listener = listener;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            report(n);
            return n;
        }

        @Override
        public int read() throws IOException {
            int n = super.read();
            report(n == -1 ? -1 : 1);
            return n;
        }

        private void report(int n) {
            if (n <= 0) return;
            downloaded += n;

            // ✅ cada ~2MB
            if (downloaded - lastReported >= 2_000_000) {
                lastReported = downloaded;
                if (listener != null) listener.onProgress(downloaded, totalBytes);
            }
        }
    }
}

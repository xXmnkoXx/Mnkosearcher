package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.infra.importacion.PythonExportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

@Component
public class DescargarPythonExportUseCase {

    private static final String FILE_NACIONAL  = "nacional.ndjson.gz";
    private static final String FILE_CATALUNYA = "catalunya.ndjson.gz";

    private final PythonExportClient client;

    /**
     * Puede ser relativa (importsPython) o absoluta. Funciona igual.
     */
    private final Path downloadDir;

    public DescargarPythonExportUseCase(
            PythonExportClient client,
            @Value("${python.export.download-dir:work/python-exports}") String downloadDir
    ) {
        this.client = client;
        this.downloadDir = Paths.get(downloadDir).normalize(); // mantener relativa si quieres
    }

    public Resultado descargarNacional(int yearMin, Integer limit, Integer batchRows) {
        Map<String, String> p = new HashMap<>();
        p.put("yearMin", String.valueOf(yearMin));
        if (limit != null && limit > 0) p.put("limit", String.valueOf(limit));
        if (batchRows != null && batchRows > 0) p.put("batchRows", String.valueOf(batchRows));

        return descargar("nacional", p, FILE_NACIONAL);
    }

    public Resultado descargarCatalunya(int yearMin, int yearMax, String datasets, Integer limit, Integer batchRows) {
        Map<String, String> p = new HashMap<>();
        p.put("yearMin", String.valueOf(yearMin));
        p.put("yearMax", String.valueOf(yearMax));
        if (datasets != null && !datasets.isBlank()) p.put("datasets", datasets);
        if (limit != null && limit > 0) p.put("limit", String.valueOf(limit));
        if (batchRows != null && batchRows > 0) p.put("batchRows", String.valueOf(batchRows));

        return descargar("catalunya", p, FILE_CATALUNYA);
    }

    private Resultado descargar(String source, Map<String, String> params, String fixedFileName) {
        Instant t0 = Instant.now();
        Path partPath = null;

        try {
            Path base = downloadDir.normalize();              // relativa ok
            Path baseAbs = base.toAbsolutePath().normalize(); // para mkdir y seguridad
            Files.createDirectories(baseAbs);

            // ✅ Nombre final FIJO
            Path finalPath = base.resolve(fixedFileName).normalize();

            // ✅ Part ÚNICO para evitar choques si se llama 2 veces a la vez
            partPath = base.resolve(fixedFileName + ".part-" + UUID.randomUUID()).normalize();

            // ✅ Seguridad (compara en absoluto)
            Path finalAbs = finalPath.toAbsolutePath().normalize();
            Path partAbs  = partPath.toAbsolutePath().normalize();
            if (!finalAbs.startsWith(baseAbs) || !partAbs.startsWith(baseAbs)) {
                throw new IllegalStateException("Ruta de descarga inválida");
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try (InputStream in = client.openNdjsonStream(source, params);
                 OutputStream fout = Files.newOutputStream(
                         partPath,
                         StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING,
                         StandardOpenOption.WRITE
                 );
                 DigestOutputStream digOut = new DigestOutputStream(fout, md);
                 GZIPOutputStream gz = new GZIPOutputStream(digOut)) {

                byte[] buf = new byte[1024 * 64];
                int n;
                while ((n = in.read(buf)) != -1) {
                    gz.write(buf, 0, n);
                }
                gz.finish();
            }

            // ✅ Mueve a definitivo (sobrescribe siempre el fijo)
            try {
                Files.move(partPath, finalPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(partPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            }

            long sizeBytes = Files.size(finalPath);
            String sha256 = toHex(md.digest());
            long ms = Duration.between(t0, Instant.now()).toMillis();

            return new Resultado(true, source, fixedFileName, finalPath.toString(), sizeBytes, sha256, ms);

        } catch (Exception e) {
            try { if (partPath != null) Files.deleteIfExists(partPath); } catch (Exception ignored) {}
            throw new RuntimeException("No se pudo descargar export Python (" + source + "): " + e.getMessage(), e);
        }
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    public record Resultado(
            boolean ok,
            String source,
            String fileName,
            String fullPath,
            long sizeBytes,
            String sha256,
            long durationMs
    ) {}
}

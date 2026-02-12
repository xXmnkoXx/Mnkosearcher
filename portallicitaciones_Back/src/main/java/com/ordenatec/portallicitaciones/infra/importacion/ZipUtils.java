package com.ordenatec.portallicitaciones.infra.importacion;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipUtils {

    private static final String GLOBAL_ROOT =
            "licitacionesPerfilesContratanteCompleto3.atom";

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Extrae SOLO archivos .atom preservando nombres.
     */
    public void extractAtomFiles(InputStream zipStream, Path outputDir) throws IOException {

        Files.createDirectories(outputDir);

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (!entry.getName().endsWith(".atom")) {
                    continue;
                }

                Path outFile = outputDir.resolve(entry.getName()).normalize();

                if (!outFile.startsWith(outputDir)) {
                    throw new IOException("ZIP PATH TRAVERSAL detectado: " + entry.getName());
                }

                Files.createDirectories(outFile.getParent());
                Files.copy(zis, outFile, StandardCopyOption.REPLACE_EXISTING);

                zis.closeEntry();
            }
        }
    }

    /**
     * Localiza el ATOM raíz correcto dentro de un directorio.
     */
    public Path detectRootAtom(Path atomDirectory) throws IOException {

        if (!Files.isDirectory(atomDirectory)) {
            throw new IllegalArgumentException("No es un directorio: " + atomDirectory);
        }

        // 1) Prioridad absoluta: atom global
        Path globalRoot = atomDirectory.resolve(GLOBAL_ROOT);
        if (Files.exists(globalRoot)) {
            return globalRoot;
        }

        // 2) Buscar root por timestamp (sin _n)
        List<Path> candidates = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(atomDirectory, "*.atom")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();

                // Excluir partes _1, _2, etc
                if (name.matches(".*_\\d+\\.atom")) {
                    continue;
                }

                // Debe tener timestamp YYYYMMDD_HHMMSS
                if (name.matches(".*_\\d{8}_\\d{6}\\.atom")) {
                    candidates.add(p);
                }
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No se encontró ningún ATOM raíz válido en " + atomDirectory);
        }

        // Elegir el más reciente
        return candidates.stream()
                .max(Comparator.comparing(this::extractTimestamp))
                .orElseThrow();
    }

    private LocalDateTime extractTimestamp(Path atomFile) {

        String name = atomFile.getFileName().toString();

        // ejemplo: licitacionesPerfilesContratanteCompleto3_20260116_190524.atom
        int idx = name.lastIndexOf("_");
        int dot = name.lastIndexOf(".atom");

        String ts = name.substring(idx + 1, dot);

        return LocalDateTime.parse(ts, TS_FORMAT);
    }
}

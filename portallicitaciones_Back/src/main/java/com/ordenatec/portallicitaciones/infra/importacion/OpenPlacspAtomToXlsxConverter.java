package com.ordenatec.portallicitaciones.infra.importacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenPlacspAtomToXlsxConverter {

    private static final Logger log = LoggerFactory.getLogger(OpenPlacspAtomToXlsxConverter.class);

    @Value("${openplacsp.home}")
    private String toolHome;

    @Value("${openplacsp.jar:open-placsp-2.2-complete.jar}")
    private String jarName;

    @Value("${openplacsp.mode:2}")
    private int mode;

    /**
     * Convierte un .atom a .xlsx usando el JDK embebido en OpenPLACSP (con JavaFX),
     * pero ejecutando el jar en modo automático (sin usar el .bat para evitar GUI).
     */
    public void convert(Path atomFile, Path xlsxFile) {
        try {
            if (atomFile == null || !Files.exists(atomFile)) {
                throw new IllegalArgumentException("ATOM no existe: " + atomFile);
            }

            Files.createDirectories(xlsxFile.getParent());

            Path home = Path.of(toolHome).toAbsolutePath().normalize();
            Path javaExe = home.resolve("jdk").resolve("bin").resolve(isWindows() ? "java.exe" : "java");
            Path jarPath = home.resolve(jarName);

            if (!Files.exists(javaExe)) {
                throw new IllegalStateException("No existe java embebido en OpenPLACSP: " + javaExe);
            }
            if (!Files.exists(jarPath)) {
                throw new IllegalStateException("No existe el jar de OpenPLACSP: " + jarPath);
            }

            List<String> cmd = new ArrayList<>();
            cmd.add(javaExe.toString());
            cmd.add("-jar");
            cmd.add(jarPath.toString());
            cmd.add("--input");
            cmd.add(atomFile.toAbsolutePath().toString());
            cmd.add("--output");
            cmd.add(xlsxFile.toAbsolutePath().toString());
            cmd.add("--mode");
            cmd.add(String.valueOf(mode));

            log.info("OpenPLACSP cmd: {}", String.join(" ", cmd));
            log.info("OpenPLACSP workdir: {}", home);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(home.toFile());           // IMPORTANTE: así encuentra el jar y recursos
            pb.redirectErrorStream(true);          // junta stderr+stdout para leerlo sin bloqueos

            Process p = pb.start();

            // Lee salida para depurar (y para que no se bloquee por buffer lleno)
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.info("[OpenPLACSP] {}", line);
                }
            }

            boolean finished = p.waitFor(Duration.ofMinutes(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new RuntimeException("OpenPLACSP timeout (>10 min). Proceso abortado.");
            }

            int exit = p.exitValue();
            if (exit != 0) {
                throw new RuntimeException("OpenPLACSP terminó con error. ExitCode=" + exit);
            }

            if (!Files.exists(xlsxFile) || Files.size(xlsxFile) == 0) {
                throw new RuntimeException("OpenPLACSP terminó OK pero NO generó el XLSX: " + xlsxFile);
            }

            log.info("OpenPLACSP XLSX generado: {} ({} bytes)", xlsxFile, Files.size(xlsxFile));

        } catch (Exception e) {
            throw new RuntimeException("Fallo convirtiendo ATOM->XLSX con OpenPLACSP", e);
        }
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }
}

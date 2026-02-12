package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sustituto HEADLESS de OpenPlacspAtomToXlsxConverter (sin JavaFX).
 *
 * Uso:
 *  converter.convert(atomPath, xlsxPath);
 *
 * Internamente:
 *  - Lee el .atom con AtomFeedReaderStax (ya integrado en tu app)
 *  - Genera un XLSX "estilo OpenPLACSP" con Apache POI
 *  - Termina el proceso (no queda esperando input humano)
 */
@Component
public class OpenPlacspHeadlessConverter {

    private static final Logger log = LoggerFactory.getLogger(OpenPlacspHeadlessConverter.class);

    private final OpenPlacspAtomExporter exporter;

    public OpenPlacspHeadlessConverter(OpenPlacspAtomExporter exporter) {
        this.exporter = exporter;
    }

    public void convert(Path atomFile, Path xlsxOut) {
        try {
            if (atomFile == null || !Files.exists(atomFile)) {
                throw new IllegalArgumentException("ATOM no existe: " + atomFile);
            }
            Files.createDirectories(xlsxOut.getParent());

            log.info("OpenPLACSP HEADLESS: exportando ATOM => XLSX");
            log.info("ATOM : {}", atomFile.toAbsolutePath());
            log.info("XLSX : {}", xlsxOut.toAbsolutePath());

            exporter.export(atomFile, xlsxOut);

            long size = Files.size(xlsxOut);
            log.info("OpenPLACSP HEADLESS: OK ({} bytes) => {}", size, xlsxOut.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Fallo en OpenPLACSP HEADLESS convert(). atom=" + atomFile + " out=" + xlsxOut, e);
        }
    }
}

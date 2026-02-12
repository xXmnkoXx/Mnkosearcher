package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Crea un XLSX con las 3 hojas del export de OpenPLACSP y expone:
 * - sheets
 * - estilos
 * - writeTo(path)
 *
 * Se usa desde OpenPlacspAtomExporter (headless).
 */
public class OpenPlacspXlsxWriter implements AutoCloseable {

    private final Workbook wb;

    private final Sheet licitaciones;
    private final Sheet encargos;
    private final Sheet consultas;

    private final CellStyle textStyle;
    private final CellStyle numberStyle;
    private final CellStyle dateStyle;
    private final CellStyle dateTimeStyle;

    public OpenPlacspXlsxWriter() {
        this.wb = new XSSFWorkbook();

        // Sheets
        this.licitaciones = wb.createSheet(OpenPlacspXlsxSchema.SHEET_LICITACIONES);
        this.encargos = wb.createSheet(OpenPlacspXlsxSchema.SHEET_ENCARGOS);
        this.consultas = wb.createSheet(OpenPlacspXlsxSchema.SHEET_CONSULTAS);

        // Styles
        DataFormat fmt = wb.createDataFormat();

        this.textStyle = wb.createCellStyle();
        // (sin formato especial)

        this.numberStyle = wb.createCellStyle();
        this.numberStyle.setDataFormat(fmt.getFormat("#,##0.00"));

        this.dateStyle = wb.createCellStyle();
        this.dateStyle.setDataFormat(fmt.getFormat("yyyy-mm-dd"));

        this.dateTimeStyle = wb.createCellStyle();
        this.dateTimeStyle.setDataFormat(fmt.getFormat("yyyy-mm-dd hh:mm:ss"));
    }

    public Sheet licitacionesSheet() {
        return licitaciones;
    }

    public Sheet encargosSheet() {
        return encargos;
    }

    public Sheet consultasSheet() {
        return consultas;
    }

    public CellStyle textStyle() {
        return textStyle;
    }

    public CellStyle numberStyle() {
        return numberStyle;
    }

    public CellStyle dateStyle() {
        return dateStyle;
    }

    public CellStyle dateTimeStyle() {
        return dateTimeStyle;
    }

    public void writeTo(Path out) {
        try {
            Files.createDirectories(out.getParent());
            try (OutputStream os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo escribir XLSX: " + out.toAbsolutePath(), e);
        }
    }

    @Override
    public void close() {
        try {
            wb.close();
        } catch (Exception ignored) {
        }
    }
}

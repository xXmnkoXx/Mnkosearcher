package com.ordenatec.portallicitaciones.infra.importacion;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class PlcspXlsxParser {

    private static final String SHEET_LICITACIONES = "Licitaciones";

    public List<PlcspRow> parse(byte[] xlsxBytes) {
        Objects.requireNonNull(xlsxBytes, "xlsxBytes");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {

            Sheet sheet = wb.getSheet(SHEET_LICITACIONES);
            if (sheet == null && wb.getNumberOfSheets() > 0) {
                sheet = wb.getSheetAt(0);
            }
            if (sheet == null) return List.of();

            DataFormatter fmt = new DataFormatter(Locale.ROOT, true);
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return List.of();

            List<String> headers = readHeaders(headerRow, fmt, eval);
            if (headers.isEmpty()) return List.of();

            List<PlcspRow> out = new ArrayList<>(Math.max(0, sheet.getLastRowNum()));

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> cols = new LinkedHashMap<>();
                boolean any = false;

                for (int c = 0; c < headers.size(); c++) {
                    String h = headers.get(c);
                    if (h == null || h.isBlank()) continue;

                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String v = readCellAsString(cell, fmt, eval);

                    if (v != null) {
                        v = v.trim();
                        if (!v.isEmpty()) {
                            cols.put(h, v);
                            any = true;
                        }
                    }
                }

                if (!any) continue;
                out.add(new PlcspRow(cols));
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException("PLCSP XLSX parse error: " + e.getMessage(), e);
        }
    }

    private static List<String> readHeaders(Row headerRow, DataFormatter fmt, FormulaEvaluator eval) {
        List<String> headers = new ArrayList<>();
        short last = headerRow.getLastCellNum();
        if (last < 0) return headers;

        for (int c = 0; c < last; c++) {
            Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String h = readCellAsString(cell, fmt, eval);
            headers.add(h == null ? "" : h.trim());
        }
        return headers;
    }

    private static String readCellAsString(Cell cell, DataFormatter fmt, FormulaEvaluator eval) {
        if (cell == null) return null;
        try {
            return fmt.formatCellValue(cell, eval);
        } catch (Exception e) {
            try {
                return cell.toString();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    // ==========================================================
    // Row wrapper con getters tipados
    // ==========================================================
    public static final class PlcspRow {
        private final Map<String, String> cols;

        public PlcspRow(Map<String, String> cols) {
            this.cols = Collections.unmodifiableMap(new LinkedHashMap<>(cols));
        }

        public Map<String, String> cols() {
            return cols;
        }

        public String get(String header) {
            if (header == null) return null;
            return cols.get(header);
        }

        public String getTrim(String header) {
            String v = get(header);
            return v == null ? null : v.trim();
        }

        /**
         * Soporta:
         * - "12345.67"
         * - "12.345,67"
         * - "12345.67 EUR"
         * - "12345,67 €"
         */
        public BigDecimal getBigDecimal(String header) {
            String v = getTrim(header);
            if (v == null || v.isBlank()) return null;

            // quita moneda y símbolos
            String cleaned = v
                    .replace("EUR", "")
                    .replace("€", "")
                    .replace("\u00A0", " ")
                    .trim();

            // si viene con miles en formato ES "12.345,67"
            // -> quitamos puntos y cambiamos coma por punto
            // si viene "12345.67" (punto decimal), esto lo rompería,
            // así que detectamos si hay coma.
            if (cleaned.contains(",")) {
                cleaned = cleaned.replace(".", "").replace(",", ".");
            }

            // elimina espacios internos
            cleaned = cleaned.replace(" ", "");

            try {
                return new BigDecimal(cleaned);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Soporta:
         * - ISO: "2026-01-17"
         * - ES:  "17/01/2026"
         */
        public LocalDate getLocalDate(String header) {
            String v = getTrim(header);
            if (v == null || v.isBlank()) return null;

            try {
                return LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {}

            try {
                DateTimeFormatter dmy = DateTimeFormatter.ofPattern("d/M/uuuu");
                return LocalDate.parse(v, dmy);
            } catch (Exception ignored) {}

            // a veces Excel/POI puede dar "17-01-2026"
            try {
                DateTimeFormatter dmy2 = DateTimeFormatter.ofPattern("d-M-uuuu");
                return LocalDate.parse(v, dmy2);
            } catch (Exception ignored) {}

            return null;
        }
    }
}

package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import org.apache.poi.ss.usermodel.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Writer por cabeceras: permite setear valores por nombre de columna.
 *
 * IMPORTANTE:
 * - OpenPLACSP tiene cabeceras duplicadas (ej: "Número de expediente" aparece 2 veces).
 * - Esta clase NO debe petar si hay duplicados.
 * - Estrategia: nos quedamos con la PRIMERA aparición del header (putIfAbsent).
 */
public final class OpenPlacspRowWriter {

    private OpenPlacspRowWriter() {}

    public static SheetRowWriter forHeaders(
            Sheet sheet,
            List<String> headers,
            CellStyle textStyle,
            CellStyle numberStyle,
            CellStyle dateStyle,
            CellStyle dateTimeStyle
    ) {
        return new SheetRowWriter(sheet, headers, textStyle, numberStyle, dateStyle, dateTimeStyle);
    }

    public static final class SheetRowWriter {

        private final Sheet sheet;
        private final Map<String, Integer> headerToIndex; // header -> first column index
        private final CellStyle textStyle;
        private final CellStyle numberStyle;
        private final CellStyle dateStyle;
        private final CellStyle dateTimeStyle;

        private int rowIndex;

        public SheetRowWriter(
                Sheet sheet,
                List<String> headers,
                CellStyle textStyle,
                CellStyle numberStyle,
                CellStyle dateStyle,
                CellStyle dateTimeStyle
        ) {
            this.sheet = Objects.requireNonNull(sheet, "sheet");
            this.textStyle = textStyle;
            this.numberStyle = numberStyle;
            this.dateStyle = dateStyle;
            this.dateTimeStyle = dateTimeStyle;

            List<String> safeHeaders = headers != null ? headers : List.of();

            // 1) Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < safeHeaders.size(); i++) {
                String h = safeHeaders.get(i);
                Cell c = headerRow.createCell(i, CellType.STRING);
                c.setCellValue(h);
                if (this.textStyle != null) c.setCellStyle(this.textStyle);
            }

            // 2) header -> index (first occurrence)
            Map<String, Integer> tmp = new LinkedHashMap<>();
            for (int i = 0; i < safeHeaders.size(); i++) {
                String key = safeHeaders.get(i);
                if (key == null) continue;
                tmp.putIfAbsent(key, i);
            }
            this.headerToIndex = Collections.unmodifiableMap(tmp);

            this.rowIndex = 1;
        }

        public Row newRow() {
            return sheet.createRow(rowIndex++);
        }

        // --------------------
        // Setters por header
        // --------------------

        public void setText(Row row, String header, String value) {
            Integer idx = headerToIndex.get(header);
            if (idx == null) return;
            if (value == null || value.isBlank()) return;

            Cell c = getOrCreateCell(row, idx);
            c.setCellType(CellType.STRING);
            c.setCellValue(value);
            if (textStyle != null) c.setCellStyle(textStyle);
        }

        public void setBooleanText(Row row, String header, Boolean value, String trueText, String falseText) {
            if (value == null) return;
            setText(row, header, value ? trueText : falseText);
        }

        public void setInt(Row row, String header, Integer value) {
            Integer idx = headerToIndex.get(header);
            if (idx == null) return;
            if (value == null) return;

            Cell c = getOrCreateCell(row, idx);
            c.setCellType(CellType.NUMERIC);
            c.setCellValue(value);
            if (numberStyle != null) c.setCellStyle(numberStyle);
        }

        public void setNumber(Row row, String header, BigDecimal value) {
            Integer idx = headerToIndex.get(header);
            if (idx == null) return;
            if (value == null) return;

            Cell c = getOrCreateCell(row, idx);
            c.setCellType(CellType.NUMERIC);
            c.setCellValue(value.doubleValue());
            if (numberStyle != null) c.setCellStyle(numberStyle);
        }

        public void setNumber(Row row, String header, Double value) {
            Integer idx = headerToIndex.get(header);
            if (idx == null) return;
            if (value == null) return;

            Cell c = getOrCreateCell(row, idx);
            c.setCellType(CellType.NUMERIC);
            c.setCellValue(value);
            if (numberStyle != null) c.setCellStyle(numberStyle);
        }

        public void setDate(Row row, String header, LocalDate date) {
            Integer idx = headerToIndex.get(header);
            if (idx == null) return;
            if (date == null) return;

            Cell c = getOrCreateCell(row, idx);
            c.setCellType(CellType.NUMERIC);
            c.setCellValue(java.sql.Date.valueOf(date));
            if (dateStyle != null) c.setCellStyle(dateStyle);
        }

        public void setDateTime(Row row, String header, Instant instant, ZoneId zone) {
            Integer idx = headerToIndex.get(header);
            if (idx == null) return;
            if (instant == null) return;

            ZoneId z = zone != null ? zone : ZoneId.systemDefault();
            LocalDateTime ldt = LocalDateTime.ofInstant(instant, z);

            Cell c = getOrCreateCell(row, idx);
            c.setCellType(CellType.NUMERIC);
            c.setCellValue(java.sql.Timestamp.valueOf(ldt));
            if (dateTimeStyle != null) c.setCellStyle(dateTimeStyle);
        }

        public void setDateTime(Row row, String header, LocalDateTime ldt) {
            Integer idx = headerToIndex.get(header);
            if (idx == null) return;
            if (ldt == null) return;

            Cell c = getOrCreateCell(row, idx);
            c.setCellType(CellType.NUMERIC);
            c.setCellValue(java.sql.Timestamp.valueOf(ldt));
            if (dateTimeStyle != null) c.setCellStyle(dateTimeStyle);
        }

        // --------------------
        // Util
        // --------------------

        private static Cell getOrCreateCell(Row row, int idx) {
            if (row == null) throw new IllegalArgumentException("row is null");
            Cell c = row.getCell(idx);
            if (c == null) c = row.createCell(idx);
            return c;
        }
    }
}

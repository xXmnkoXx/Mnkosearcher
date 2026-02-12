package com.ordenatec.portallicitaciones.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Component
public class ImportarCatalunyaNdjsonUseCase {

    private static final int TARGET_YEAR = 2026;

    private final JdbcTemplate jdbc;
    private final ObjectMapper om;

    public ImportarCatalunyaNdjsonUseCase(JdbcTemplate jdbc, ObjectMapper om) {
        this.jdbc = jdbc;
        this.om = om;
    }

    public record Resultado(
            boolean ok,
            String source,
            boolean reset,
            int batchSize,
            int targetYear,
            long lineasLeidas,
            long filasInsertadas,
            long filasConError,
            long filasSaltadasPorYear,
            String primerError,
            String primerErrorLinea,
            long durationMs
    ) {}

    /**
     * Importa catalunya.ndjson.gz -> dbo.licitacionesCatalunya
     * SOLO targetYear (2026)
     */
    public Resultado ejecutar(Path gzFile, boolean reset, int batchSize) {

        Instant t0 = Instant.now();

        if (batchSize < 100) batchSize = 100;
        if (batchSize > 50_000) batchSize = 50_000;

        if (reset) {
            // Borra TODO catalunya (como pediste)
            jdbc.execute("TRUNCATE TABLE dbo.licitacionesCatalunya");
        }

        long lineas = 0;
        long insertadas = 0;
        long errores = 0;
        long saltadasPorYear = 0;

        String primerError = null;
        String primerErrorLinea = null;

        // mismas columnas que tu tabla (sin el ID identity)
        final String[] COLS = new String[] {
                "_dataset",
                "_import_year",
                "altres_eines_licitacio_electronica",
                "codi_ambit",
                "codi_cpv",
                "codi_departament_ens",
                "codi_dir3",
                "codi_expedient",
                "codi_ine10",
                "codi_nuts",
                "codi_organ",
                "codi_unitat",
                "data_adjudicacio_contracte",
                "data_formalitzacio_contracte",
                "data_publicacio_adjudicacio",
                "data_publicacio_anul",
                "data_publicacio_anunci",
                "data_publicacio_avaluacio",
                "data_publicacio_consulta_mercat",
                "data_publicacio_contracte_agregat",
                "data_publicacio_encarrec",
                "data_publicacio_formalitzacio",
                "data_publicacio_futura",
                "data_publicacio_previ",
                "denominacio",
                "denominacio_adjudicatari",
                "descripcio_lot",
                "durada_contracte",
                "eina_presentacio_electronica",
                "enllac_publicacio",
                "es_agregada",
                "fase_publicacio",
                "identificacio_adjudicatari",
                "import_adjudicacio_amb_iva",        // DECIMAL
                "import_adjudicacio_sense_iva",      // DECIMAL
                "lloc_execucio",
                "no_admet_eina_licitacio_electronica",
                "nom_ambit",
                "nom_departament_ens",
                "nom_organ",
                "nom_unitat",
                "numero_lot",
                "objecte_contracte",
                "ofertes_rebudes",
                "pressupost_licitacio_amb_iva",
                "pressupost_licitacio_amb_iva_expedient",
                "pressupost_licitacio_sense_iva",
                "pressupost_licitacio_sense_iva_expedient",
                "procediment",
                "racionalitzacio_contractacio",
                "resultat",
                "termini_presentacio_ofertes",
                "tipus_contracte",
                "tipus_empresa",
                "tipus_financament",
                "tipus_identificacio_adjudicatari",
                "tipus_tramitacio",
                "valor_estimat_contracte",            // DECIMAL
                "valor_estimat_expedient"             // DECIMAL
        };

        final String sql = buildInsertSql("dbo.licitacionesCatalunya", COLS);

        final ArrayList<JsonNode> batch = new ArrayList<>(batchSize);

        try (var fin = java.nio.file.Files.newInputStream(gzFile);
             var gz = new GZIPInputStream(fin);
             var br = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                lineas++;

                try {
                    JsonNode node = om.readTree(line);

                    int y = readInt(node.get("_import_year"));
                    if (y != TARGET_YEAR) {
                        saltadasPorYear++;
                        continue;
                    }

                    batch.add(node);

                    if (batch.size() >= batchSize) {
                        insertadas += flushBatch(sql, COLS, batch);
                        batch.clear();
                    }

                } catch (Exception ex) {
                    errores++;
                    if (primerError == null) {
                        primerError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                        primerErrorLinea = line.length() > 500 ? line.substring(0, 500) + "..." : line;
                    }
                }
            }

            if (!batch.isEmpty()) {
                insertadas += flushBatch(sql, COLS, batch);
                batch.clear();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error importando Catalunya NDJSON: " + e.getMessage(), e);
        }

        long ms = Duration.between(t0, Instant.now()).toMillis();
        return new Resultado(
                true,
                gzFile.toString(),
                reset,
                batchSize,
                TARGET_YEAR,
                lineas,
                insertadas,
                errores,
                saltadasPorYear,
                primerError,
                primerErrorLinea,
                ms
        );
    }

    private long flushBatch(String sql, String[] cols, List<JsonNode> rows) {
        int[] counts = jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                JsonNode n = rows.get(i);

                for (int c = 0; c < cols.length; c++) {
                    String col = cols[c];

                    // 1) _import_year INT
                    if ("_import_year".equals(col)) {
                        int y = readInt(n.get(col));
                        ps.setInt(c + 1, y);
                        continue;
                    }

                    // 2) DECIMALs (evita 8114)
                    if (isDecimalCol(col)) {
                        BigDecimal bd = readDecimal(n.get(col));
                        if (bd == null) ps.setNull(c + 1, Types.DECIMAL);
                        else ps.setBigDecimal(c + 1, bd);
                        continue;
                    }

                    // 3) Resto -> NVARCHAR
                    JsonNode v = n.get(col);
                    if (v == null || v.isNull()) {
                        // si tu tabla tiene NOT NULL en algún campo y te da guerra,
                        // puedes cambiar esto a "" en esos campos concretos.
                        ps.setNull(c + 1, Types.NVARCHAR);
                    } else {
                        String s = v.asText();
                        if (s == null) {
                            ps.setNull(c + 1, Types.NVARCHAR);
                        } else {
                            s = s.trim();
                            if (s.isEmpty()) ps.setNull(c + 1, Types.NVARCHAR);
                            else ps.setString(c + 1, s);
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        long inserted = 0;
        for (int x : counts) {
            if (x > 0) inserted += x;
        }
        return inserted;
    }

    private static boolean isDecimalCol(String col) {
        return "import_adjudicacio_amb_iva".equals(col)
                || "import_adjudicacio_sense_iva".equals(col)
                || "valor_estimat_contracte".equals(col)
                || "valor_estimat_expedient".equals(col);
    }

    private static int readInt(JsonNode v) {
        if (v == null || v.isNull()) return 0;
        if (v.isInt() || v.isLong()) return v.asInt();
        try {
            return Integer.parseInt(v.asText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Parser tolerante:
     * - "1.234,56" -> 1234.56
     * - "1234,56"  -> 1234.56
     * - "1234.56"  -> 1234.56
     * - "1 234,56 €" -> 1234.56
     */
    private static BigDecimal readDecimal(JsonNode v) {
        if (v == null || v.isNull()) return null;

        if (v.isNumber()) {
            try {
                return v.decimalValue();
            } catch (Exception ignored) {
                return null;
            }
        }

        String s = v.asText();
        if (s == null) return null;
        s = s.replace('\u00A0', ' ').trim();
        if (s.isEmpty()) return null;

        // deja solo dígitos + separadores + signo
        s = s.replaceAll("[^0-9,\\.\\-]", "");

        if (s.isEmpty() || "-".equals(s)) return null;

        boolean hasDot = s.contains(".");
        boolean hasComma = s.contains(",");

        // si tiene ambos: "." miles y "," decimal
        if (hasDot && hasComma) {
            s = s.replace(".", "");
            s = s.replace(",", ".");
        } else if (hasComma) {
            // solo coma: decimal
            s = s.replace(",", ".");
        }

        // valida forma final
        if (!s.matches("-?\\d+(\\.\\d+)?")) return null;

        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildInsertSql(String table, String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append(" (");
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("[").append(cols[i]).append("]");
        }
        sb.append(") VALUES (");
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }
}

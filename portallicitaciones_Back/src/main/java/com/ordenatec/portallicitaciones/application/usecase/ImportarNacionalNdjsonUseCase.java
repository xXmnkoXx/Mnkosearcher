package com.ordenatec.portallicitaciones.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPInputStream;

@Component
public class ImportarNacionalNdjsonUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportarNacionalNdjsonUseCase.class);

    private static final DateTimeFormatter ISO_LOCAL_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public ImportarNacionalNdjsonUseCase(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public record Resultado(
            boolean ok,
            String source,
            boolean reset,
            int batchSize,
            long lineasLeidas,
            long filasInsertadas,
            long filasConError,
            long ms
    ) {}

    /**
     * Ejecuta importación desde un fichero .ndjson.gz
     *
     * - Si filePath es null/blank:
     *   1) intenta ./importsPython/nacional.ndjson.gz (ruta relativa al working dir)
     *   2) si no existe, intenta classpath: importsPython/nacional.ndjson.gz (dentro del jar)
     */
    public Resultado ejecutar(String filePath, boolean reset, int batchSize) throws IOException, SQLException {

        String sourceName;
        InputStream in;

        if (filePath != null && !filePath.isBlank()) {
            Path p = Paths.get(filePath);
            sourceName = p.toAbsolutePath().toString();
            in = Files.newInputStream(p);
        } else {
            Path p = Paths.get("importsPython", "nacional.ndjson.gz");
            if (Files.exists(p)) {
                sourceName = p.toAbsolutePath().toString();
                in = Files.newInputStream(p);
            } else {
                // fallback classpath (funciona en jar)
                ClassPathResource res = new ClassPathResource("importsPython/nacional.ndjson.gz");
                sourceName = "classpath:importsPython/nacional.ndjson.gz";
                in = res.getInputStream();
            }
        }

        try (in) {
            return ejecutar(in, sourceName, reset, batchSize);
        }
    }

    public Resultado ejecutar(InputStream gzNdjsonStream, String sourceName, boolean reset, int batchSize) throws IOException, SQLException {
        long t0 = System.currentTimeMillis();

        if (batchSize <= 0) batchSize = 1000;

        long lineas = 0;
        long ok = 0;
        long err = 0;

        String insertSql = """
            INSERT INTO dbo.licitacionesNacional (
                id, expediente, objeto, organo_contratante, nif_organo, dir3_organo, id_plataforma, ciudad_organo, dependencia,
                tipo_contrato_code, tipo_contrato, subtipo_code, procedimiento_code, procedimiento, estado_code, estado,
                importe_sin_iva, importe_con_iva, importe_adjudicacion, importe_adj_con_iva,
                adjudicatario, nif_adjudicatario, num_ofertas, es_pyme,
                cpv_principal, cpvs, ubicacion, nuts,
                duracion, duracion_unidad, financiacion_ue, urgencia,
                fecha_limite, hora_limite, fecha_adjudicacion, fecha_publicacion, fecha_updated,
                url, conjunto, archivo_origen, ano, tipo_registro,
                id_consulta, nombre_consulta, condiciones, tipo_condicion,
                fecha_planificada, fecha_limite_respuestas
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            """;

        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);

            if (reset) {
                log.warn("TRUNCATE dbo.licitacionesNacional ...");
                try (Statement st = con.createStatement()) {
                    st.execute("TRUNCATE TABLE dbo.licitacionesNacional");
                }
                con.commit();
            }

            try (
                    GZIPInputStream gz = new GZIPInputStream(gzNdjsonStream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8), 1 << 20);
                    PreparedStatement ps = con.prepareStatement(insertSql)
            ) {
                int inBatch = 0;

                String line;
                while ((line = br.readLine()) != null) {
                    lineas++;
                    if (line.isBlank()) continue;

                    try {
                        JsonNode n = objectMapper.readTree(line);

                        int i = 1;

                        // strings
                        ps.setString(i++, s(n, "id", 256, true));
                        ps.setString(i++, s(n, "expediente", 50, false));
                        ps.setString(i++, s(n, "objeto", 2000, true));
                        ps.setString(i++, s(n, "organo_contratante", 300, true));
                        ps.setString(i++, s(n, "nif_organo", 15, false));
                        ps.setString(i++, s(n, "dir3_organo", 15, false));
                        ps.setString(i++, s(n, "id_plataforma", 32, false));
                        ps.setString(i++, s(n, "ciudad_organo", 50, false));
                        ps.setString(i++, s(n, "dependencia", 800, false));

                        ps.setString(i++, s(n, "tipo_contrato_code", 10, false));
                        ps.setString(i++, s(n, "tipo_contrato", 50, false));
                        ps.setString(i++, s(n, "subtipo_code", 10, false));
                        ps.setString(i++, s(n, "procedimiento_code", 10, false));
                        ps.setString(i++, s(n, "procedimiento", 50, true));
                        ps.setString(i++, s(n, "estado_code", 10, true));
                        ps.setString(i++, s(n, "estado", 20, true));

                        // decimals
                        setBigDecimal(ps, i++, bd(n, "importe_sin_iva"));
                        setBigDecimal(ps, i++, bd(n, "importe_con_iva"));
                        setBigDecimal(ps, i++, bd(n, "importe_adjudicacion"));
                        setBigDecimal(ps, i++, bd(n, "importe_adj_con_iva"));

                        ps.setString(i++, s(n, "adjudicatario", 200, false));
                        ps.setString(i++, s(n, "nif_adjudicatario", 40, false));

                        // int / bit
                        setInteger(ps, i++, intOrNull(n, "num_ofertas"));
                        setBooleanBit(ps, i++, boolOrNull(n, "es_pyme"));

                        ps.setString(i++, s(n, "cpv_principal", 16, false));
                        ps.setString(i++, s(n, "cpvs", 4000, false));
                        ps.setString(i++, s(n, "ubicacion", 50, false));
                        ps.setString(i++, s(n, "nuts", 10, false));

                        setBigDecimal(ps, i++, bd(n, "duracion"));
                        ps.setString(i++, s(n, "duracion_unidad", 10, false));
                        ps.setString(i++, s(n, "financiacion_ue", 10, false));
                        setBigDecimal(ps, i++, bd(n, "urgencia"));

                        // fechas/horas
                        setTimestamp(ps, i++, tsFlexible(n, "fecha_limite"));
                        setTime(ps, i++, timeOrNull(n, "hora_limite"));
                        setTimestamp(ps, i++, tsFlexible(n, "fecha_adjudicacion"));
                        setTimestamp(ps, i++, tsFlexible(n, "fecha_publicacion"));
                        setTimestamp(ps, i++, tsFlexible(n, "fecha_updated"));

                        ps.setString(i++, s(n, "url", 256, true));
                        ps.setString(i++, s(n, "conjunto", 20, true));
                        ps.setString(i++, s(n, "archivo_origen", 100, true));

                        // año / tipo_registro
                        setShort(ps, i++, shortOrNull(n, "ano"), true);
                        ps.setString(i++, s(n, "tipo_registro", 10, true));

                        ps.setString(i++, s(n, "id_consulta", 50, false));
                        ps.setString(i++, s(n, "nombre_consulta", 800, false));
                        ps.setString(i++, s(n, "condiciones", 1200, false));
                        ps.setString(i++, s(n, "tipo_condicion", 10, false));

                        setTimestamp(ps, i++, tsFlexible(n, "fecha_planificada"));
                        setTimestamp(ps, i++, tsFlexible(n, "fecha_limite_respuestas"));

                        ps.addBatch();
                        inBatch++;

                        if (inBatch >= batchSize) {
                            ok += flushBatch(ps, con, inBatch);
                            inBatch = 0;
                        }

                    } catch (Exception ex) {
                        err++;
                        // Log cada X errores para no llenar la consola
                        if (err <= 20 || (err % 500 == 0)) {
                            log.warn("Línea {} con error: {}", lineas, ex.getMessage());
                        }
                    }
                }

                if (inBatch > 0) {
                    ok += flushBatch(ps, con, inBatch);
                }
            }
        }

        long ms = System.currentTimeMillis() - t0;

        return new Resultado(true, sourceName, reset, batchSize, lineas, ok, err, ms);
    }

    private long flushBatch(PreparedStatement ps, Connection con, int inBatch) throws SQLException {
        int[] r = ps.executeBatch();
        con.commit();
        ps.clearBatch();
        // algunos drivers devuelven SUCCESS_NO_INFO (-2)
        long count = 0;
        for (int v : r) {
            if (v > 0) count += v;
            else if (v == Statement.SUCCESS_NO_INFO) count += 1;
        }
        // fallback: si devolvió 0 pero se ejecutó, asumimos inBatch
        return count == 0 ? inBatch : count;
    }

    private static String s(JsonNode n, String field, int maxLen, boolean required) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            if (required) return null; // dejamos que SQL lance error si columna NOT NULL
            return null;
        }
        String text = v.asText(null);
        if (text == null) return null;
        text = text.trim();
        if (text.isEmpty() || "NaT".equalsIgnoreCase(text)) return null;
        if (text.length() > maxLen) return text.substring(0, maxLen);
        return text;
    }

    private static BigDecimal bd(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.decimalValue();
        String s = v.asText(null);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || "NaT".equalsIgnoreCase(s)) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer intOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.intValue();
        String s = v.asText(null);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean boolOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isBoolean()) return v.booleanValue();
        String s = v.asText(null);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s) || "0".equals(s) || "no".equalsIgnoreCase(s)) return false;
        return null;
    }

    private static Timestamp tsFlexible(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;

        String s = v.asText(null);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || "NaT".equalsIgnoreCase(s)) return null;

        try {
            // formato con 'T' (2026-01-16T00:00:00 o con microsegundos)
            if (s.contains("T")) {
                LocalDateTime dt = LocalDateTime.parse(s, ISO_LOCAL_DT);
                return Timestamp.valueOf(dt);
            }
            // formato fecha (2025-08-19)
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return Timestamp.valueOf(d.atStartOfDay());
            }
            // fallback (si viniese con espacio)
            if (s.contains(" ")) {
                // 2026-01-16 00:00:00
                LocalDateTime dt = LocalDateTime.parse(s.replace(" ", "T"));
                return Timestamp.valueOf(dt);
            }
        } catch (Exception ignore) {}

        return null;
    }

    private static LocalTime timeOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;

        String s = v.asText(null);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || "NaT".equalsIgnoreCase(s)) return null;

        try {
            return LocalTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static void setBigDecimal(PreparedStatement ps, int idx, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.DECIMAL);
        else ps.setBigDecimal(idx, v);
    }

    private static void setInteger(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.INTEGER);
        else ps.setInt(idx, v);
    }

    private static void setShort(PreparedStatement ps, int idx, Short v, boolean required) throws SQLException {
        if (v == null) {
            if (required) ps.setNull(idx, java.sql.Types.SMALLINT);
            else ps.setNull(idx, java.sql.Types.SMALLINT);
        } else {
            ps.setShort(idx, v);
        }
    }

    private static Short shortOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return (short) v.intValue();
        String s = v.asText(null);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Short.parseShort(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static void setBooleanBit(PreparedStatement ps, int idx, Boolean v) throws SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.BIT);
        else ps.setBoolean(idx, v);
    }

    private static void setTimestamp(PreparedStatement ps, int idx, Timestamp ts) throws SQLException {
        if (ts == null) ps.setNull(idx, java.sql.Types.TIMESTAMP);
        else ps.setTimestamp(idx, ts);
    }

    private static void setTime(PreparedStatement ps, int idx, LocalTime t) throws SQLException {
        if (t == null) ps.setNull(idx, java.sql.Types.TIME);
        else ps.setTime(idx, Time.valueOf(t));
    }
}

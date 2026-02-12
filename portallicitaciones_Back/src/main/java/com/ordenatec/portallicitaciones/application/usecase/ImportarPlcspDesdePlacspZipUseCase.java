package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.infra.importacion.HttpZipDownloader;
import com.ordenatec.portallicitaciones.infra.importacion.PlcspXlsxParser;
import com.ordenatec.portallicitaciones.infra.importacion.PlcspXlsxParser.PlcspRow;
import com.ordenatec.portallicitaciones.infra.importacion.PlcspZipSourceResolver;
import com.ordenatec.portallicitaciones.infra.importacion.ZipUtils;
import com.ordenatec.portallicitaciones.infra.importacion.plcsp.OpenPlacspHeadlessConverter;
import com.ordenatec.portallicitaciones.infra.importacion.plcsp.PlcspLicitacionMapper;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ImportarPlcspDesdePlacspZipUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportarPlcspDesdePlacspZipUseCase.class);

    public record Resultado(
            boolean ok,
            String zipUrl,
            String atomRootFile,
            String xlsxFile,
            int filasLeidas,
            int insertadas,
            int actualizadas,
            int saltadasSinClave,
            int errores
    ) {}

    private final PlcspZipSourceResolver zipSourceResolver;
    private final HttpZipDownloader httpZipDownloader;
    private final ZipUtils zipUtils;
    private final OpenPlacspHeadlessConverter atomToXlsxConverter;
    private final PlcspXlsxParser parser;
    private final LicitacionJpaRepository licitacionJpa;
    private final PlcspLicitacionMapper mapper;

    @Value("${plcsp.workdir:work/plcsp}")
    private String workDir;

    public ImportarPlcspDesdePlacspZipUseCase(
            PlcspZipSourceResolver zipSourceResolver,
            HttpZipDownloader httpZipDownloader,
            ZipUtils zipUtils,
            OpenPlacspHeadlessConverter atomToXlsxConverter,
            PlcspXlsxParser parser,
            LicitacionJpaRepository licitacionJpa,
            PlcspLicitacionMapper mapper
    ) {
        this.zipSourceResolver = zipSourceResolver;
        this.httpZipDownloader = httpZipDownloader;
        this.zipUtils = zipUtils;
        this.atomToXlsxConverter = atomToXlsxConverter;
        this.parser = parser;
        this.licitacionJpa = licitacionJpa;
        this.mapper = mapper;
    }

    @Transactional
    public Resultado ejecutarMesActual() {
        YearMonth ym = YearMonth.now();
        URI zipUrl = zipSourceResolver.resolverZipPorMes(ym.getYear(), ym.getMonthValue());
        return ejecutar(zipUrl, ym);
    }

    @Transactional
    public Resultado ejecutarMes(int year, int month1to12) {
        YearMonth ym = YearMonth.of(year, month1to12);
        URI zipUrl = zipSourceResolver.resolverZipPorMes(year, month1to12);
        return ejecutar(zipUrl, ym);
    }

    /**
     * Import PLCSP:
     * - Descarga ZIP mensual de PLACSP (sindicacion)
     * - Extrae ATOM
     * - Convierte ATOM a XLSX con OpenPLACSP (headless)
     * - Parse XLSX a filas
     * - IMPORTANTE: agrupa filas por licitación (expediente o link) para no “machacar” 1 licitación por lote
     * - Upsert por grupo (1 save por licitación)
     */
    private Resultado ejecutar(URI zipUrl, YearMonth ym) {

        int insertadas = 0;
        int actualizadas = 0;
        int saltadas = 0;
        int errores = 0;

        Path atomRoot = null;
        Path xlsxPath = null;
        int filasLeidas = 0;

        try {
            Path monthDir = Path.of(workDir).resolve(ym.toString()); // "2026-01"
            Files.createDirectories(monthDir);

            // 1) Descargar ZIP
            log.info("PLCSP: descargando ZIP {}", zipUrl);
            try (InputStream zipStream = httpZipDownloader.descargar(zipUrl, null)) {

                // 2) Extraer .atom
                log.info("PLCSP: extrayendo .atom en {}", monthDir.toAbsolutePath());
                zipUtils.extractAtomFiles(zipStream, monthDir);
            }

            // 3) Detectar ATOM raíz
            atomRoot = zipUtils.detectRootAtom(monthDir);
            log.info("PLCSP: ATOM raíz detectado => {}", atomRoot.getFileName());

            // 4) Convertir ATOM raíz a XLSX
            xlsxPath = monthDir.resolve("placsp_" + ym + ".xlsx");
            log.info("PLCSP: generando XLSX (HEADLESS) => {}", xlsxPath.toAbsolutePath());
            atomToXlsxConverter.convert(atomRoot, xlsxPath);

            // 5) Parsear XLSX -> filas completas por cabecera
            byte[] xlsxBytes = Files.readAllBytes(xlsxPath);
            List<PlcspRow> rows = parser.parse(xlsxBytes);
            filasLeidas = rows.size();

            // 6) AGRUPAR filas por licitación (clave = expediente si existe; si no, url)
            Map<String, List<PlcspRow>> grupos = new LinkedHashMap<>();
            for (PlcspRow r : rows) {
                String expediente = safeTrim(r.getTrim("Número de expediente"));
                String url = safeTrim(r.getTrim("Link licitación"));

                String key = !isBlank(expediente) ? "EXP:" + expediente
                        : (!isBlank(url) ? "URL:" + url : null);

                if (key == null) {
                    saltadas++;
                    continue;
                }

                grupos.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(r);
            }

            log.info("PLCSP: filasLeidas={} grupos={}", filasLeidas, grupos.size());

            // 7) UPSERT por grupo (1 licitación por grupo)
            for (Map.Entry<String, List<PlcspRow>> entry : grupos.entrySet()) {
                String groupKey = entry.getKey();
                List<PlcspRow> groupRows = entry.getValue();
                PlcspRow first = groupRows.get(0);

                try {
                    String expediente = safeTrim(first.getTrim("Número de expediente"));
                    String url = safeTrim(first.getTrim("Link licitación"));

                    // Buscar existente por expediente, si no, por URL
                    Optional<LicitacionEntity> existing = Optional.empty();
                    if (!isBlank(expediente)) {
                        existing = licitacionJpa.findByExpediente(expediente);
                    }
                    if (existing.isEmpty() && !isBlank(url)) {
                        existing = licitacionJpa.findByUrlPublica(url);
                    }

                    boolean isNew = existing.isEmpty();
                    LicitacionEntity e = existing.orElseGet(LicitacionEntity::new);

                    if (isNew) {
                        e.setUuid(UUID.randomUUID());
                    }

                    // Asegura claves mínimas
                    if (!isBlank(expediente)) e.setExpediente(expediente);
                    if (!isBlank(url)) e.setUrlPublica(url);

                    // Mapeo base (título, fechas, importes, estado, procedimiento, etc.)
                    mapper.mapIntoEntity(first, e);

                    // Tracking
                    e.setFechaUltimaActualizacion(Instant.now());

                    /*
                      🔧 IMPORTANTE (para “guardar TODOS los datos posibles”):
                      En PLCSP OpenPLACSP muchas columnas vienen por LOTE. En tu modelo ya tienes tablas colindantes
                      (cpvs, lotes, adjudicacion, organismo). Este use case ahora ya agrupa por licitación,
                      así que aquí es donde conviene enriquecer a partir de groupRows:

                      - CPVs: suelen venir en columna "CPV" (y a veces "CPV licitación/lote")
                      - Lotes: columna "Lote" + importes/objeto por lote
                      - Adjudicación: columnas de adjudicatario/importe adjudicación/fecha
                      - Organismo: columnas "Órgano de Contratación", "NIF OC", "DIR3", "ID OC en PLACSP"

                      Como no me has pegado tus entities/repos aquí, NO invento nombres de métodos.
                      Pero el punto de extensión correcto es ESTE (con groupRows).
                    */

                    licitacionJpa.save(e);

                    if (isNew) insertadas++;
                    else actualizadas++;

                } catch (Exception ex) {
                    errores++;
                    log.warn("PLCSP: error procesando grupo {}: {}", groupKey, ex.toString());
                }
            }

            return new Resultado(
                    true,
                    zipUrl.toString(),
                    atomRoot.getFileName().toString(),
                    xlsxPath.toString(),
                    filasLeidas,
                    insertadas,
                    actualizadas,
                    saltadas,
                    errores
            );

        } catch (Exception e) {
            log.error("PLCSP: fallo importando {}: {}", zipUrl, e.toString());
            return new Resultado(
                    false,
                    zipUrl.toString(),
                    atomRoot != null ? atomRoot.getFileName().toString() : null,
                    xlsxPath != null ? xlsxPath.toString() : null,
                    filasLeidas,
                    insertadas,
                    actualizadas,
                    saltadas,
                    errores + 1
            );
        }
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

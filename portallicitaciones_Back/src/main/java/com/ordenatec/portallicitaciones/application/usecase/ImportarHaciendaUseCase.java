package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.domain.model.LicitacionEvento;
import com.ordenatec.portallicitaciones.domain.port.LicitacionEventoRepository;
import com.ordenatec.portallicitaciones.domain.port.LicitacionRepository;
import com.ordenatec.portallicitaciones.infra.importacion.AtomEntryParseResult;
import com.ordenatec.portallicitaciones.infra.importacion.AtomFeedReaderStax;
import com.ordenatec.portallicitaciones.infra.importacion.HaciendaAtomSourceResolver;
import com.ordenatec.portallicitaciones.infra.importacion.HttpZipDownloader;
import com.ordenatec.portallicitaciones.infra.persistence.adapter.LicitacionUpsertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ImportarHaciendaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportarHaciendaUseCase.class);
    private static final int LOG_EVERY_N = 500;

    private final HaciendaAtomSourceResolver sourceResolver;
    private final HttpZipDownloader zipDownloader;
    private final AtomFeedReaderStax atomFeedReader;

    private final LicitacionUpsertService upsertService;
    private final LicitacionRepository licitacionRepository;
    private final LicitacionEventoRepository licitacionEventoRepository;

    public ImportarHaciendaUseCase(
            HaciendaAtomSourceResolver sourceResolver,
            HttpZipDownloader zipDownloader,
            AtomFeedReaderStax atomFeedReader,
            LicitacionUpsertService upsertService,
            LicitacionRepository licitacionRepository,
            LicitacionEventoRepository licitacionEventoRepository
    ) {
        this.sourceResolver = sourceResolver;
        this.zipDownloader = zipDownloader;
        this.atomFeedReader = atomFeedReader;
        this.upsertService = upsertService;
        this.licitacionRepository = licitacionRepository;
        this.licitacionEventoRepository = licitacionEventoRepository;
    }

    public Resultado ejecutar() {
        var zipUri = sourceResolver.resolverZipMasReciente();

        int atomProcesados = 0;
        int entradasLeidas = 0;

        int licitacionesUpsert = 0;

        int eventosGuardados = 0;
        int eventosDuplicados = 0;
        int entradasSaltadasSinExpediente = 0;

        Instant inicio = Instant.now();
        log.info("Importación Hacienda INICIO. zip={}", zipUri);

        Instant inicioDescarga = Instant.now();
        AtomicLong lastLogAtMs = new AtomicLong(0);

        // Cache: expediente -> licitacionUuid (UUID)
        Map<String, UUID> cacheExpedienteToUuid = new HashMap<>(50_000);

        try (InputStream zipStream = zipDownloader.descargar(zipUri, (done, total) -> {
                 long nowMs = Duration.between(inicioDescarga, Instant.now()).toMillis();
                 long prev = lastLogAtMs.get();
                 if (nowMs - prev < 2000) return;
                 lastLogAtMs.set(nowMs);

                 if (total > 0) {
                     long pct = (done * 100) / total;
                     log.info("Descargando ZIP: {}% ({} / {} MB)", pct, toMb(done), toMb(total));
                 } else {
                     log.info("Descargando ZIP: {} MB", toMb(done));
                 }
             });
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            long msDescarga = Duration.between(inicioDescarga, Instant.now()).toMillis();
            log.info("ZIP descargado y abierto. Tiempo descarga={} ms", msDescarga);

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                if (entryName == null || !entryName.toLowerCase(Locale.ROOT).endsWith(".atom")) {
                    zis.closeEntry();
                    continue;
                }

                atomProcesados++;
                Instant inicioAtom = Instant.now();

                // ✅ CLAVE: el reader NO puede cerrar el ZipInputStream
                InputStream atomStream = new NonClosingInputStream(zis);

                List<AtomEntryParseResult> resultados =
                        atomFeedReader.leerConEventos(atomStream, zipUri.toString(), entryName);

                entradasLeidas += resultados.size();

                for (AtomEntryParseResult res : resultados) {

                    Licitacion lic = res.licitacion();
                    String expediente = (lic != null ? lic.getExpediente() : null);

                    // ✅ Si no hay expediente, no podemos hacer upsert ni enlazar evento -> saltamos
                    if (expediente == null || expediente.isBlank()) {
                        entradasSaltadasSinExpediente++;
                        continue;
                    }

                    // 1) Upsert licitación (puede lanzar si expediente no válido)
                    try {
                        upsertService.upsert(lic);
                        licitacionesUpsert++;
                    } catch (IllegalArgumentException ex) {
                        // No tires toda la importación por 1 entry malo
                        entradasSaltadasSinExpediente++;
                        continue;
                    }

                    // 2) Resolver UUID de la licitación por expediente (cache)
                    UUID licUuid = cacheExpedienteToUuid.get(expediente);
                    if (licUuid == null) {
                        licUuid = licitacionRepository.findIdByExpediente(expediente)
                                .orElseGet(() -> licitacionRepository.findByExpediente(expediente)
                                        .map(Licitacion::getId)
                                        .orElseThrow(() -> new IllegalStateException(
                                                "No encuentro licitación tras upsert. expediente=" + expediente + " entry=" + entryName
                                        )));
                        cacheExpedienteToUuid.put(expediente, licUuid);
                    }

                    // 3) Guardar evento SIEMPRE enlazado a licitación (si existe)
                    LicitacionEvento ev = res.evento();
                    if (ev != null) {
                        ev.setLicitacionId(licUuid);

                        try {
                            licitacionEventoRepository.save(ev);
                            eventosGuardados++;
                        } catch (DataIntegrityViolationException ex) {
                            eventosDuplicados++;
                        }
                    }

                    if ((licitacionesUpsert + eventosGuardados) % LOG_EVERY_N == 0) {
                        long ms = Duration.between(inicio, Instant.now()).toMillis();
                        log.info("Progreso -> atomsProcesados={}, entradasLeidas={}, licitacionesUpsert={}, eventosGuardados={}, eventosDuplicados={}, saltadasSinExpediente={}, cacheExpedientes={}, tiempo={} ms",
                                atomProcesados, entradasLeidas, licitacionesUpsert, eventosGuardados, eventosDuplicados,
                                entradasSaltadasSinExpediente, cacheExpedienteToUuid.size(), ms);
                    }
                }

                long msAtom = Duration.between(inicioAtom, Instant.now()).toMillis();
                log.info("ATOM procesado -> {} en {} ms. Totales -> licitacionesUpsert={}, eventosGuardados={}, eventosDuplicados={}, saltadasSinExpediente={}, cacheExpedientes={}",
                        entryName, msAtom, licitacionesUpsert, eventosGuardados, eventosDuplicados, entradasSaltadasSinExpediente, cacheExpedienteToUuid.size());

                zis.closeEntry();
            }

            long msTotal = Duration.between(inicio, Instant.now()).toMillis();
            log.info("Importación Hacienda FIN OK. atomsProcesados={}, entradasLeidas={}, licitacionesUpsert={}, eventosGuardados={}, eventosDuplicados={}, saltadasSinExpediente={}, total={} ms",
                    atomProcesados, entradasLeidas, licitacionesUpsert, eventosGuardados, eventosDuplicados, entradasSaltadasSinExpediente, msTotal);

            return new Resultado(
                    zipUri.toString(),
                    atomProcesados,
                    entradasLeidas,
                    licitacionesUpsert,
                    eventosGuardados,
                    eventosDuplicados,
                    entradasSaltadasSinExpediente
            );

        } catch (Exception e) {
            log.error("Importación Hacienda ERROR. zip={}", zipUri, e);
            throw new RuntimeException("Error importando licitaciones", e);
        }
    }

    private static long toMb(long bytes) {
        if (bytes <= 0) return 0;
        return bytes / (1024L * 1024L);
    }

    /** ✅ Evita que el AtomFeedReader cierre el ZipInputStream */
    static final class NonClosingInputStream extends FilterInputStream {
        NonClosingInputStream(InputStream in) { super(in); }
        @Override public void close() throws IOException { /* NO-OP */ }
    }

    public record Resultado(
            String sourceZip,
            int atomProcesados,
            int entradasLeidas,
            int licitacionesUpsert,
            int eventosGuardados,
            int eventosDuplicados,
            int entradasSaltadasSinExpediente
    ) {}
}

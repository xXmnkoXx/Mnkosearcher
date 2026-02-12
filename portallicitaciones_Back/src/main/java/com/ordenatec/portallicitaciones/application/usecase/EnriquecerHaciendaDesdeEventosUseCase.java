package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.domain.model.Cpv;
import com.ordenatec.portallicitaciones.domain.model.Documento;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.domain.model.Lote;
import com.ordenatec.portallicitaciones.domain.model.Money;
import com.ordenatec.portallicitaciones.infra.importacion.AtomEntryParseResult;
import com.ordenatec.portallicitaciones.infra.importacion.AtomEntryReaderStax;
import com.ordenatec.portallicitaciones.infra.persistence.adapter.LicitacionUpsertService;
import com.ordenatec.portallicitaciones.infra.persistence.entity.DocumentoEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEventoEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LoteEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionEventoJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class EnriquecerHaciendaDesdeEventosUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnriquecerHaciendaDesdeEventosUseCase.class);

    public record Resultado(
            int eventosTotal,
            int eventosEnVentana,
            int eventosSinRawXml,
            int eventosParseOk,
            int eventosParseError,
            int licitacionesUpsertOk,
            int licitacionesNoEncontradasEnDb,
            int docsInsertados,
            int lotesInsertados
    ) {}

    private final LicitacionEventoJpaRepository eventoRepo;
    private final AtomEntryReaderStax entryReader;
    private final LicitacionUpsertService upsertService;
    private final LicitacionJpaRepository licitacionRepo;

    public EnriquecerHaciendaDesdeEventosUseCase(
            LicitacionEventoJpaRepository eventoRepo,
            AtomEntryReaderStax entryReader,
            LicitacionUpsertService upsertService,
            LicitacionJpaRepository licitacionRepo
    ) {
        this.eventoRepo = eventoRepo;
        this.entryReader = entryReader;
        this.upsertService = upsertService;
        this.licitacionRepo = licitacionRepo;
    }

    @Transactional
    public Resultado ejecutar(int daysBack) {
        int effective = daysBack > 0 ? daysBack : 7;
        Instant from = Instant.now().minus(effective, ChronoUnit.DAYS);

        List<LicitacionEventoEntity> all = eventoRepo.findAll();
        int total = all.size();

        List<LicitacionEventoEntity> window = all.stream()
                .filter(ev -> {
                    Instant t = bestEventInstant(ev);
                    return t != null && !t.isBefore(from);
                })
                .sorted(Comparator.comparing(EnriquecerHaciendaDesdeEventosUseCase::bestEventInstant).reversed())
                .toList();

        int enVentana = window.size();
        int sinRaw = 0;
        int parseOk = 0;
        int parseErr = 0;
        int upsertOk = 0;
        int noEncontradas = 0;

        int docsInsertados = 0;
        int lotesInsertados = 0;

        log.info("[HACIENDA-ENRICH] INICIO daysBack={} (effective={}) eventosTotal={} eventosVentana={}",
                daysBack, effective, total, enVentana);

        for (LicitacionEventoEntity ev : window) {
            String rawEntryXml = ev.getRawEntryXml();
            if (rawEntryXml == null || rawEntryXml.isBlank()) {
                sinRaw++;
                continue;
            }

            // ✅ CAMBIO: AtomEntryReaderStax devuelve null si no puede parsear (no rompe el batch)
            AtomEntryParseResult parsed;
            try {
                parsed = entryReader.parseSingleEntry(rawEntryXml);
            } catch (Exception unexpected) {
                // Solo para errores NO esperados (no el parse normal)
                parseErr++;
                log.warn(
                        "[HACIENDA-ENRICH] Unexpected ERROR eventoId={} expediente={} ex={} msg={}",
                        ev.getId(),
                        ev.getExpediente(),
                        unexpected.getClass().getName(),
                        unexpected.getMessage(),
                        unexpected
                );
                continue;
            }

            if (parsed == null || parsed.licitacion() == null) {
                parseErr++;
                log.warn("[HACIENDA-ENRICH] Parse NULL/ERROR eventoId={} expediente={}",
                        ev.getId(), ev.getExpediente());
                continue;
            }

            Licitacion lic = parsed.licitacion();
            parseOk++;

            // 1) Upsert cabecera
            LicitacionEntity saved;
            try {
                saved = upsertService.upsert(lic);
                upsertOk++;
            } catch (Exception e) {
                log.warn(
                        "[HACIENDA-ENRICH] Upsert ERROR expediente={} ex={} msg={}",
                        safe(lic.getExpediente()),
                        e.getClass().getName(),
                        e.getMessage(),
                        e
                );
                continue;
            }

            // 2) Cargar entidad gestionada y enriquecer hijos
            Optional<LicitacionEntity> managedOpt = licitacionRepo.findByExpediente(saved.getExpediente());
            if (managedOpt.isEmpty()) {
                noEncontradas++;
                continue;
            }

            LicitacionEntity managed = managedOpt.get();

            docsInsertados += applyDocumentos(managed, lic);
            lotesInsertados += applyLotes(managed, lic);

            licitacionRepo.save(managed);
        }

        log.info("[HACIENDA-ENRICH] FIN total={} ventana={} sinRaw={} parseOk={} parseErr={} upsertOk={} noEncontradas={} docs={} lotes={}",
                total, enVentana, sinRaw, parseOk, parseErr, upsertOk, noEncontradas, docsInsertados, lotesInsertados);

        return new Resultado(total, enVentana, sinRaw, parseOk, parseErr, upsertOk, noEncontradas, docsInsertados, lotesInsertados);
    }

    private static Instant bestEventInstant(LicitacionEventoEntity ev) {
        if (ev == null) return null;
        if (ev.getAtomUpdatedAt() != null) return ev.getAtomUpdatedAt();
        if (ev.getAtomPublishedAt() != null) return ev.getAtomPublishedAt();
        return ev.getFecha();
    }

    /**
     * Idempotente: borra docs existentes y vuelve a insertar los del domain.
     * Devuelve cuántos docs se han insertado.
     */
    private static int applyDocumentos(LicitacionEntity managed, Licitacion lic) {
        managed.getDocumentos().clear();

        if (lic.getDocumentos() == null || lic.getDocumentos().isEmpty()) return 0;

        int count = 0;
        for (Documento d : lic.getDocumentos()) {
            DocumentoEntity de = new DocumentoEntity();
            de.setLicitacion(managed);

            de.setTipo(safe(d.getTipo()));
            de.setTitulo(safe(d.getTitulo()));
            de.setUrl(safe(d.getUrl()));
            de.setHash(safe(d.getHash()));

            // Inferir formato (pdf/docx/zip/...) de la URL si se puede
            de.setFormato(inferFormatoFromUrl(d.getUrl()));

            managed.getDocumentos().add(de);
            count++;
        }
        return count;
    }

    /**
     * Idempotente: borra lotes existentes y vuelve a insertar los del domain.
     * Devuelve cuántos lotes se han insertado.
     */
    private static int applyLotes(LicitacionEntity managed, Licitacion lic) {
        managed.getLotes().clear();

        if (lic.getLotes() == null || lic.getLotes().isEmpty()) return 0;

        int count = 0;
        int fallback = 1;

        for (Lote l : lic.getLotes()) {
            LoteEntity le = new LoteEntity();
            le.setLicitacion(managed);

            le.setNumero(parseNumero(safe(l.getNumero()), fallback));
            le.setTitulo(safe(l.getTitulo()));

            Money p = l.getPresupuesto();
            if (p != null) {
                BigDecimal amount = p.getAmount();
                String currency = p.getCurrency();
                Boolean incIva = p.getIncluyeIVA();

                le.setImporte(amount);
                le.setMoneda(safe(currency));
                le.setIncluyeIva(incIva);
            }

            // CPV principal del lote (si hay)
            if (l.getCpvs() != null && !l.getCpvs().isEmpty()) {
                Cpv first = l.getCpvs().get(0);
                le.setCpvPrincipal(safe(first.getCodigo()));
            }

            managed.getLotes().add(le);
            count++;
            fallback++;
        }

        return count;
    }

    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer parseNumero(String raw, int fallback) {
        if (raw == null) return fallback;
        String digits = raw.replaceAll("\\D+", "");
        if (digits.isBlank()) return fallback;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String inferFormatoFromUrl(String url) {
        if (url == null) return null;
        String u = url.toLowerCase();

        // quita querystring
        int q = u.indexOf('?');
        if (q >= 0) u = u.substring(0, q);

        if (u.endsWith(".pdf")) return "pdf";
        if (u.endsWith(".doc") || u.endsWith(".docx")) return "docx";
        if (u.endsWith(".xls") || u.endsWith(".xlsx")) return "xlsx";
        if (u.endsWith(".zip")) return "zip";
        if (u.endsWith(".xml")) return "xml";
        if (u.endsWith(".html") || u.endsWith(".htm")) return "html";

        return null;
    }
}

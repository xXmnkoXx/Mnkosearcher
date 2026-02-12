package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.infra.detalle.DetalleExtraido;
import com.ordenatec.portallicitaciones.infra.detalle.EuskadiDetalleExtractor;
import com.ordenatec.portallicitaciones.infra.http.DetalleHttpClient;
import com.ordenatec.portallicitaciones.infra.persistence.entity.CpvEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEventoEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.CpvJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionEventoJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class EnriquecerDetalleEuskadiUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnriquecerDetalleEuskadiUseCase.class);

    // Euskadi HTML público
    private static final String EUSKADI_HOST = "contratacion.euskadi.eus";

    public record Resultado(
            int eventosEuskadiLeidos,
            int licitacionesUnicas,
            int licitacionesSinUrlPublica,
            int httpOk,
            int httpError,
            int actualizadas,
            int sinCambios
    ) {}

    private final LicitacionJpaRepository licRepo;
    private final LicitacionEventoJpaRepository eventoRepo;
    private final CpvJpaRepository cpvRepo;
    private final DetalleHttpClient http;
    private final EuskadiDetalleExtractor extractor;

    public EnriquecerDetalleEuskadiUseCase(
            LicitacionJpaRepository licRepo,
            LicitacionEventoJpaRepository eventoRepo,
            CpvJpaRepository cpvRepo,
            DetalleHttpClient http,
            EuskadiDetalleExtractor extractor
    ) {
        this.licRepo = licRepo;
        this.eventoRepo = eventoRepo;
        this.cpvRepo = cpvRepo;
        this.http = http;
        this.extractor = extractor;
    }

    /**
     * Enriquecimiento de Euskadi:
     * - Fuente de verdad para detectar Euskadi: licitacion_eventos.url_publica
     * - No dependemos del atom_entry_id para Euskadi (solo es sindicacion del Estado)
     */
    @Transactional
    public Resultado ejecutar(int daysBack, boolean force) {
        int effective = daysBack > 0 ? daysBack : 30;
        Instant from = Instant.now().minus(effective, ChronoUnit.DAYS);

        // OJO: este método lo añadiremos en la SIGUIENTE clase (repo)
        List<LicitacionEventoEntity> eventos = eventoRepo.findEuskadiUltimosEventosPorLicitacion(from, "%" + EUSKADI_HOST + "%");

        int eventosLeidos = eventos.size();

        // licitaciones únicas a procesar
        LinkedHashSet<Long> licIds = new LinkedHashSet<>();
        for (LicitacionEventoEntity ev : eventos) {
            if (ev.getLicitacion() != null && ev.getLicitacion().getId() != null) {
                licIds.add(ev.getLicitacion().getId());
            }
        }

        int licitacionesUnicas = licIds.size();
        int sinUrlPublica = 0;
        int httpOk = 0;
        int httpErr = 0;
        int actualizadas = 0;
        int sinCambios = 0;

        log.info("[DETALLE-EUSKADI] INICIO daysBack={} force={} eventosEuskadi={} licitacionesUnicas={}",
                effective, force, eventosLeidos, licitacionesUnicas);

        for (Long licId : licIds) {
            Optional<LicitacionEntity> opt = licRepo.findById(licId);
            if (opt.isEmpty()) continue;

            LicitacionEntity lic = opt.get();

            String urlPublica = trimToNull(lic.getUrlPublica());
            if (urlPublica == null || !urlPublica.contains(EUSKADI_HOST)) {
                // por si url_publica solo está en el evento, la rellenaremos en el repo en siguiente paso
                sinUrlPublica++;
                continue;
            }

            // si ya tengo detalle y no force => no repetir
            if (!force && trimToNull(lic.getRawDetalleXml()) != null) {
                sinCambios++;
                continue;
            }

            // url_detalle: guardamos el mismo HTML público
            if (trimToNull(lic.getUrlDetalle()) == null) {
                lic.setUrlDetalle(urlPublica);
            }

            DetalleHttpClient.FetchResult fetch = http.get(urlPublica);
            if (fetch == null || !fetch.ok() || fetch.body() == null || fetch.body().isBlank()) {
                httpErr++;
                continue;
            }
            httpOk++;

            // guardar HTML bruto (para depurar/parsing futuro)
            lic.setRawDetalleXml(fetch.body());

            // parsear
            DetalleExtraido extra = extractor.extraer(fetch);

            boolean changed = applyExtraido(lic, extra);
            if (changed) actualizadas++;
            else sinCambios++;

            licRepo.save(lic);
        }

        log.info("[DETALLE-EUSKADI] FIN eventosEuskadi={} licUnicas={} sinUrlPublica={} httpOk={} httpErr={} actualizadas={} sinCambios={}",
                eventosLeidos, licitacionesUnicas, sinUrlPublica, httpOk, httpErr, actualizadas, sinCambios);

        return new Resultado(
                eventosLeidos,
                licitacionesUnicas,
                sinUrlPublica,
                httpOk,
                httpErr,
                actualizadas,
                sinCambios
        );
    }

    /**
     * Aplica valores extraídos sobre la licitación.
     * Devuelve true si ha cambiado algo.
     */
    private boolean applyExtraido(LicitacionEntity lic, DetalleExtraido d) {
        if (lic == null || d == null) return false;

        boolean changed = false;

        changed |= setIfChanged(lic.getEntidad(), lic::setEntidad, d.getEntidad());
        changed |= setIfChanged(lic.getEstadoTexto(), lic::setEstadoTexto, d.getEstadoTexto());
        changed |= setIfChanged(lic.getCriterioAdjudicacion(), lic::setCriterioAdjudicacion, d.getCriterioAdjudicacion());
        changed |= setIfChanged(lic.getCodigoNuts(), lic::setCodigoNuts, d.getCodigoNuts());
        changed |= setIfChanged(lic.getLugarEjecucion(), lic::setLugarEjecucion, d.getLugarEjecucion());
        changed |= setIfChanged(lic.getMotivo(), lic::setMotivo, d.getMotivo());
        changed |= setIfChanged(lic.getPlazoEjecucion(), lic::setPlazoEjecucion, d.getPlazoEjecucion());
        changed |= setIfChanged(lic.getModalidad(), lic::setModalidad, d.getModalidad());
        changed |= setIfChanged(lic.getProcedimiento(), lic::setProcedimiento, d.getProcedimiento());

        // Importes: si tu entity tiene campos distintos, lo ajustamos en el siguiente paso
        if (d.getPresupuesto() != null) {
            if (lic.getPrecioLicitacion() == null || lic.getPrecioLicitacion().compareTo(d.getPresupuesto()) != 0) {
                lic.setPrecioLicitacion(d.getPresupuesto());
                changed = true;
            }
        }
        if (d.getMoneda() != null) {
            if (lic.getMoneda() == null || !d.getMoneda().equalsIgnoreCase(lic.getMoneda())) {
                lic.setMoneda(d.getMoneda());
                changed = true;
            }
        }

        // CPVs: reemplazo total si vienen
        if (d.getCpvCodes() != null && !d.getCpvCodes().isEmpty()) {
            Set<CpvPivote> nuevos = new LinkedHashSet<>();
            for (String code : d.getCpvCodes()) {
                String c = trimToNull(code);
                if (c == null) continue;
                nuevos.add(new CpvPivote(c));
            }

            // materializa entidades CPV
            Set<CpvEntity> newEntities = new LinkedHashSet<>();
            for (CpvPivote p : nuevos) {
                CpvEntity cpv = cpvRepo.findByCodigo(p.codigo)
                        .orElseGet(() -> {
                            CpvEntity n = new CpvEntity();
                            n.setCodigo(p.codigo);
                            return n;
                        });
                cpvRepo.save(cpv);
                newEntities.add(cpv);
            }

            // Reemplazo total de relación
            if (lic.getCpvs() != null) {
                lic.getCpvs().clear();
                lic.getCpvs().addAll(newEntities);
            } else {
                // si por alguna razón es null, lo dejamos sin explotar (depende de tu entity)
                // (si te falla aquí, lo ajustamos en la siguiente clase con el mapeo real)
            }
            changed = true;
        }

        return changed;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean setIfChanged(String current, java.util.function.Consumer<String> setter, String value) {
        String v = trimToNull(value);
        if (v == null) return false;
        String c = trimToNull(current);
        if (c != null && c.equals(v)) return false;
        setter.accept(v);
        return true;
    }

    /**
     * Pequeño wrapper para asegurar unicidad de CPV en el set
     */
    private static class CpvPivote {
        final String codigo;
        CpvPivote(String codigo) { this.codigo = codigo; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CpvPivote)) return false;
            return Objects.equals(codigo, ((CpvPivote) o).codigo);
        }
        @Override public int hashCode() { return Objects.hash(codigo); }
    }
}

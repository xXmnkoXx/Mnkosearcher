package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.infra.detalle.DetalleExtraido;
import com.ordenatec.portallicitaciones.infra.detalle.EstadoDetalleExtractor;
import com.ordenatec.portallicitaciones.infra.http.DetalleHttpClient;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Component
public class EnriquecerDetalleCatalunyaUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnriquecerDetalleCatalunyaUseCase.class);

    public record Resultado(
            int licitacionesTotal,
            int licitacionesEnVentana,
            int licitacionesCatalunyaDetectadas,
            int licitacionesSinUrlPublica,
            int httpOk,
            int httpError,
            int actualizadas,
            int sinCambios
    ) {}

    private final LicitacionJpaRepository licRepo;
    private final DetalleHttpClient http;
    private final EstadoDetalleExtractor extractor;

    public EnriquecerDetalleCatalunyaUseCase(
            LicitacionJpaRepository licRepo,
            DetalleHttpClient http,
            EstadoDetalleExtractor extractor
    ) {
        this.licRepo = licRepo;
        this.http = http;
        this.extractor = extractor;
    }

    /**
     * Enriquecimiento de detalle para Cataluña (PSCP):
     * - Filtra por fecha_publicacion >= hoy - daysBack (si fecha_publicacion null, se excluye para evitar ruido)
     * - Descarga url_publica y extrae campos "tipo Estado" (entidad/organo/lugar/NUTS/plazo/estado/criterio...)
     *
     * @param daysBack ventana por fecha_publicacion
     * @param force si true re-descarga aunque ya exista raw_detalle_xml
     */
    @Transactional
    public Resultado ejecutar(int daysBack, boolean force) {
        int effective = daysBack > 0 ? daysBack : 30;
        LocalDate from = LocalDate.now().minusDays(effective);

        List<LicitacionEntity> all = licRepo.findAll();
        int total = all.size();

        // Ventana por fecha_publicacion (si null, fuera)
        List<LicitacionEntity> enVentana = all.stream()
                .filter(l -> l.getFechaPublicacion() != null && !l.getFechaPublicacion().isBefore(from))
                .toList();

        int enVentanaCount = enVentana.size();

        // Detectar Catalunya por url_publica
        List<LicitacionEntity> cat = enVentana.stream()
                .filter(l -> esUrlCatalunya(trimToNull(l.getUrlPublica())))
                .toList();

        int catCount = cat.size();

        int sinUrlPublica = 0;
        int httpOk = 0;
        int httpErr = 0;
        int actualizadas = 0;
        int sinCambios = 0;

        log.info("[DETALLE-CAT] INICIO daysBack={} force={} licTotal={} licVentana={} licCat={}",
                effective, force, total, enVentanaCount, catCount);

        for (LicitacionEntity lic : cat) {
            String urlPublica = trimToNull(lic.getUrlPublica());
            if (urlPublica == null) {
                sinUrlPublica++;
                continue;
            }

            // Si ya tengo raw_detalle_xml y no force => skip
            if (!force) {
                String rawPrev = trimToNull(lic.getRawDetalleXml());
                if (rawPrev != null) {
                    sinCambios++;
                    continue;
                }
            }

            // url_detalle = url_publica (si aún no está)
            if (trimToNull(lic.getUrlDetalle()) == null) {
                lic.setUrlDetalle(urlPublica);
            }

            DetalleHttpClient.FetchResult fetch = http.get(urlPublica);
            if (fetch == null || !fetch.ok() || fetch.body() == null) {
                httpErr++;
                continue;
            }
            httpOk++;

            // Guardamos HTML tal cual para depuración
            lic.setRawDetalleXml(fetch.body());

            // Extraemos y aplicamos
            DetalleExtraido extraido = extractor.extraer(fetch);
            boolean changed = applyExtraido(lic, extraido);

            if (changed) actualizadas++;
            else sinCambios++;

            licRepo.save(lic);
        }

        log.info("[DETALLE-CAT] FIN licTotal={} licVentana={} licCat={} sinUrlPublica={} httpOk={} httpErr={} actualizadas={} sinCambios={}",
                total, enVentanaCount, catCount, sinUrlPublica, httpOk, httpErr, actualizadas, sinCambios);

        return new Resultado(
                total,
                enVentanaCount,
                catCount,
                sinUrlPublica,
                httpOk,
                httpErr,
                actualizadas,
                sinCambios
        );
    }

    private static boolean applyExtraido(LicitacionEntity lic, DetalleExtraido d) {
        if (lic == null || d == null) return false;

        boolean changed = false;

        changed |= setIfNotNull(lic::getEntidad, lic::setEntidad, d.getEntidad());
        changed |= setIfNotNull(lic::getLugarEjecucion, lic::setLugarEjecucion, d.getLugarEjecucion());
        changed |= setIfNotNull(lic::getCodigoNuts, lic::setCodigoNuts, d.getCodigoNuts());
        changed |= setIfNotNull(lic::getPlazoEjecucion, lic::setPlazoEjecucion, d.getPlazoEjecucion());
        changed |= setIfNotNull(lic::getMotivo, lic::setMotivo, d.getMotivo());
        changed |= setIfNotNull(lic::getEstadoTexto, lic::setEstadoTexto, d.getEstadoTexto());
        changed |= setIfNotNull(lic::getCriterioAdjudicacion, lic::setCriterioAdjudicacion, d.getCriterioAdjudicacion());

        return changed;
    }

    private static boolean setIfNotNull(java.util.function.Supplier<String> getter,
                                        java.util.function.Consumer<String> setter,
                                        String value) {
        String v = trimToNull(value);
        if (v == null) return false;

        String current = trimToNull(getter.get());
        if (current != null && current.equals(v)) return false;

        setter.accept(v);
        return true;
    }

    private static boolean esUrlCatalunya(String url) {
        if (url == null) return false;
        String s = url.toLowerCase(Locale.ROOT);
        return s.contains("contractaciopublica.cat")
                || s.contains("contractaciopublica.gencat.cat")
                || s.contains("contractacio-publica")     // por si hay variantes
                || s.contains("pscp");                    // por si algún enlace lo incluye
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

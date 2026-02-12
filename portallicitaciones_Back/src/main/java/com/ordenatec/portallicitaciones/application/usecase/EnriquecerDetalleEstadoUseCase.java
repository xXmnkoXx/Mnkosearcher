package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.infra.detalle.DetalleExtraido;
import com.ordenatec.portallicitaciones.infra.detalle.EstadoDetalleExtractor;
import com.ordenatec.portallicitaciones.infra.http.DetalleHttpClient;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

@Component
public class EnriquecerDetalleEstadoUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnriquecerDetalleEstadoUseCase.class);

    public record Resultado(
            int licitacionesLeidas,
            int licitacionesEuskadi,
            int licitacionesEnVentana,
            int licitacionesSinUrl,
            int httpOk,
            int httpError,
            int estadoEncontrado,
            int actualizadas,
            int sinCambios
    ) {}

    private final LicitacionJpaRepository licRepo;
    private final DetalleHttpClient http;
    private final EstadoDetalleExtractor extractor;

    public EnriquecerDetalleEstadoUseCase(
            LicitacionJpaRepository licRepo,
            DetalleHttpClient http,
            EstadoDetalleExtractor extractor
    ) {
        this.licRepo = licRepo;
        this.http = http;
        this.extractor = extractor;
    }

    /**
     * Enriquecer SOLO el campo "estadoTexto" de licitaciones de Euskadi
     * descargando el HTML de detalle desde url_detalle / url_publica.
     *
     * @param daysBack ventana por fechaPublicacion (si existe). Si fechaPublicacion es null, se procesa igual.
     * @param force si true: re-descarga y re-procesa aunque ya exista estadoTexto y/o rawDetalleXml
     */
    @Transactional
    public Resultado ejecutar(int daysBack, boolean force) {
        int effective = daysBack > 0 ? daysBack : 60;

        LocalDate fromDate = LocalDate.now().minusDays(effective);
        Instant fromInstant = Instant.now().minus(effective, ChronoUnit.DAYS);

        int leidas = 0;
        int euskadi = 0;
        int enVentana = 0;
        int sinUrl = 0;
        int httpOk = 0;
        int httpErr = 0;
        int estadoEncontrado = 0;
        int actualizadas = 0;
        int sinCambios = 0;

        log.info("[EUSKADI-ESTADO] INICIO daysBack={} force={} fromDate={} fromInstant={}",
                effective, force, fromDate, fromInstant);

        int page = 0;
        int size = 200;

        while (true) {
            Page<LicitacionEntity> p = licRepo.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
            );

            if (p.isEmpty()) break;

            for (LicitacionEntity lic : p.getContent()) {
                if (lic == null) continue;
                leidas++;

                // 1) Filtrar solo Euskadi por URL (detalle o pública)
                String url = firstNonBlank(lic.getUrlDetalle(), lic.getUrlPublica());
                if (url == null) {
                    sinUrl++;
                    continue;
                }
                if (!isEuskadiUrl(url)) continue;
                euskadi++;

                // 2) Ventana temporal (si hay fechaPublicacion)
                if (lic.getFechaPublicacion() != null) {
                    if (lic.getFechaPublicacion().isBefore(fromDate)) continue;
                    enVentana++;
                } else {
                    // si no hay fechaPublicacion, no podemos filtrar por daysBack de forma fiable => lo procesamos
                    enVentana++;
                }

                // 3) Si no force, saltar si ya tiene estado
                if (!force) {
                    String estadoPrev = trimToNull(lic.getEstadoTexto());
                    if (estadoPrev != null) {
                        sinCambios++;
                        continue;
                    }
                }

                // 4) Descargar HTML
                DetalleHttpClient.FetchResult fetch = http.get(url);
                if (fetch == null || !fetch.ok() || trimToNull(fetch.body()) == null) {
                    httpErr++;
                    continue;
                }
                httpOk++;

                // 5) Guardar raw (para depurar)
                lic.setRawDetalleXml(fetch.body());
                lic.setFechaUltimaActualizacion(Instant.now());

                // 6) Extraer estado
                DetalleExtraido extraido = extractor.extraer(fetch);
                String estado = (extraido != null) ? trimToNull(extraido.getEstadoTexto()) : null;

                if (estado == null) {
                    // No encontrado en la página
                    sinCambios++;
                    licRepo.save(lic);
                    continue;
                }
                estadoEncontrado++;

                // 7) Aplicar SOLO estadoTexto
                String prev = trimToNull(lic.getEstadoTexto());
                if (prev != null && prev.equalsIgnoreCase(estado)) {
                    sinCambios++;
                } else {
                    lic.setEstadoTexto(estado);
                    actualizadas++;
                }

                licRepo.save(lic);
            }

            if (!p.hasNext()) break;
            page++;
        }

        log.info("[EUSKADI-ESTADO] FIN leidas={} euskadi={} enVentana={} sinUrl={} httpOk={} httpErr={} estadoEncontrado={} actualizadas={} sinCambios={}",
                leidas, euskadi, enVentana, sinUrl, httpOk, httpErr, estadoEncontrado, actualizadas, sinCambios);

        return new Resultado(
                leidas,
                euskadi,
                enVentana,
                sinUrl,
                httpOk,
                httpErr,
                estadoEncontrado,
                actualizadas,
                sinCambios
        );
    }

    private static boolean isEuskadiUrl(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("contratacion.euskadi.eus");
    }

    private static String firstNonBlank(String a, String b) {
        String x = trimToNull(a);
        if (x != null) return x;
        return trimToNull(b);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

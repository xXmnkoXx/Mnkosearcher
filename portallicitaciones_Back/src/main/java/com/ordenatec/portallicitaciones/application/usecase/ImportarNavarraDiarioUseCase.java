package com.ordenatec.portallicitaciones.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.infra.importacion.NavarraDatastoreClient;
import com.ordenatec.portallicitaciones.infra.importacion.NavarraRowMapper;
import com.ordenatec.portallicitaciones.infra.persistence.adapter.LicitacionUpsertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Importación "diaria" Navarra:
 * - Descarga el dump JSON del datastore
 * - Filtra por FechaPublicacion >= hoy - daysBack
 * - Upsert de licitaciones
 */
@Component
public class ImportarNavarraDiarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportarNavarraDiarioUseCase.class);

    public record Resultado(
            String sourceUrl,
            int filasLeidas,
            int filasDentroVentana,
            int licitacionesUpsert
    ) {}

    private final NavarraDatastoreClient client;
    private final NavarraRowMapper mapper;
    private final LicitacionUpsertService upsertService;

    public ImportarNavarraDiarioUseCase(
            NavarraDatastoreClient client,
            NavarraRowMapper mapper,
            LicitacionUpsertService upsertService
    ) {
        this.client = client;
        this.mapper = mapper;
        this.upsertService = upsertService;
    }

    public Resultado ejecutar(int daysBack) {
        int safeDays = Math.max(0, daysBack);

        LocalDate hoyUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate desde = hoyUtc.minusDays(safeDays);

        log.info("[NAV] Import INICIO daysBack={} desde={} (UTC)", safeDays, desde);

        JsonNode root = client.descargarDump();
        Map<String, Integer> idx = mapper.buildIndex(root.path("fields"));

        int leidas = 0;
        int dentro = 0;
        int upsert = 0;

        JsonNode records = root.path("records");
        if (!records.isArray()) {
            throw new IllegalStateException("NAV dump inesperado: root.records no es array");
        }

        for (JsonNode rec : records) {
            leidas++;

            LocalDate fechaPub = mapper.getFecha(idx, rec, "FechaPublicacion");
            if (fechaPub == null) continue;

            // Ventana: >= desde
            if (fechaPub.isBefore(desde)) continue;
            dentro++;

            Licitacion lic = mapper.toLicitacion(idx, rec);
            if (lic == null) continue;

            try {
                upsertService.upsert(lic);
                upsert++;
            } catch (Exception ex) {
                // Si quieres, puedes afinar el catch a DataIntegrityViolationException, etc.
                log.warn("[NAV] Upsert fallido expediente={} motivo={}", safeExp(lic), ex.getMessage());
            }
        }

        log.info("[NAV] Import FIN leidas={} dentroVentana={} upsert={}", leidas, dentro, upsert);

        return new Resultado(
                client.getSourceUrl(),
                leidas,
                dentro,
                upsert
        );
    }

    private static String safeExp(Licitacion lic) {
        try {
            return lic.getExpediente();
        } catch (Exception e) {
            return "?";
        }
    }
}

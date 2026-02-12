package com.ordenatec.portallicitaciones.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.infra.importacion.CatalunyaJsonExtractors;
import com.ordenatec.portallicitaciones.infra.importacion.CatalunyaLicitacionAccumulator;
import com.ordenatec.portallicitaciones.infra.importacion.CatalunyaSocrataClient;
import com.ordenatec.portallicitaciones.infra.persistence.adapter.LicitacionUpsertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Importación diaria Cataluña (Socrata ybgg-dgi6):
 * - Descarga paginada del dataset
 * - Filtra EN CÓDIGO por "hoy + últimos N días" (según tu requisito)
 * - Agrupa por codi_expedient (porque el dataset viene por lote/fase)
 * - Upsert a BD usando LicitacionUpsertService (igual que Hacienda/Euskadi)
 *
 * Nota realista:
 * - Descargar TODO el dataset sin $where puede ser enorme.
 * - Para que sea viable, el import se limita por configuración de páginas máximas a escanear.
 * - Aun así, la lógica de filtrado se hace 100% en código.
 */
@Component
public class ImportarCatalunyaDiarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportarCatalunyaDiarioUseCase.class);

    public record Resultado(
            int paginasLeidas,
            int filasLeidas,
            int filasDentroVentana,
            int expedientesAgrupados,
            int licitacionesUpsert
    ) {}

    private final CatalunyaSocrataClient client;
    private final LicitacionUpsertService upsertService;

    // Ajustes conservadores: puedes subirlos
    private static final int DEFAULT_LIMIT = 50000;
    private static final int DEFAULT_MAX_PAGES_TO_SCAN = 30; // evita intentar bajarte millones de filas

    public ImportarCatalunyaDiarioUseCase(
            CatalunyaSocrataClient client,
            LicitacionUpsertService upsertService
    ) {
        this.client = client;
        this.upsertService = upsertService;
    }

    /**
     * Import diario: hoy + daysBack.
     * daysBack=1 -> ayer y hoy.
     */
    public Resultado ejecutar(int daysBack) {
        return ejecutar(daysBack, DEFAULT_LIMIT, DEFAULT_MAX_PAGES_TO_SCAN);
    }

    /**
     * Variante parametrizable por si quieres tunear límites sin tocar código.
     */
    public Resultado ejecutar(int daysBack, int limit, int maxPagesToScan) {

        int safeDays = Math.max(daysBack, 0);
        int safeLimit = (limit <= 0) ? DEFAULT_LIMIT : limit;
        int safeMaxPages = (maxPagesToScan <= 0) ? DEFAULT_MAX_PAGES_TO_SCAN : maxPagesToScan;

        // Ventana UTC (consistente con tus otros importadores)
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = todayUtc.minusDays(safeDays);
        LocalDate toDateExclusive = todayUtc.plusDays(1);

        int offset = 0;
        int pageIndex = 0;

        int paginas = 0;
        int filasLeidas = 0;
        int filasDentro = 0;

        Map<String, Licitacion> grouped = new LinkedHashMap<>();

        while (pageIndex < safeMaxPages) {

            JsonNode page = client.fetchPage(safeLimit, offset);
            paginas++;
            pageIndex++;

            if (page == null || page.isNull() || !page.isArray() || page.isEmpty()) {
                break;
            }

            int pageCount = 0;

            for (JsonNode row : page) {
                filasLeidas++;
                pageCount++;

                LocalDate pub = CatalunyaJsonExtractors.fechaPublicacionBestEffort(row);

                // Filtrado 100% en código (tu requisito)
                if (pub == null) continue;
                if (pub.isBefore(fromDate) || !pub.isBefore(toDateExclusive)) continue;

                filasDentro++;

                // acumula (agrupa por expediente y une CPVs/lotes/etc.)
                CatalunyaLicitacionAccumulator.addRow(grouped, row);
            }

            // Si esta página trajo menos que el límite, fin de dataset
            if (pageCount < safeLimit) break;

            offset += safeLimit;

            // Log para ver avance
            if (pageIndex % 2 == 0) {
                log.info("[CAT] pages={}, rowsRead={}, rowsInWindow={}, groupedExp={}",
                        paginas, filasLeidas, filasDentro, grouped.size());
            }
        }

        int upserts = 0;
        for (Licitacion lic : grouped.values()) {
            upsertService.upsert(lic);
            upserts++;
        }

        return new Resultado(
                paginas,
                filasLeidas,
                filasDentro,
                grouped.size(),
                upserts
        );
    }
}

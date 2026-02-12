package com.ordenatec.portallicitaciones.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.ordenatec.portallicitaciones.domain.enums.TipoContrato;
import com.ordenatec.portallicitaciones.domain.model.Cpv;
import com.ordenatec.portallicitaciones.domain.model.FechasProcedimiento;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.domain.model.Money;
import com.ordenatec.portallicitaciones.domain.model.OrganismoContratacion;
import com.ordenatec.portallicitaciones.infra.importacion.EuskadiContractingNoticesClient;
import com.ordenatec.portallicitaciones.infra.importacion.EuskadiEnrichmentClient;
import com.ordenatec.portallicitaciones.infra.importacion.EuskadiJsonExtractors;
import com.ordenatec.portallicitaciones.infra.persistence.adapter.LicitacionUpsertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.util.Set;

@Component
public class ImportarEuskadiDiarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportarEuskadiDiarioUseCase.class);

    // límite real de la API
    private static final int ITEMS_OF_PAGE = 50;

    // seguridad si el endpoint ignora filtros
    private static final int MAX_PAGES_IF_FILTERS_NOT_HONORED = 400;

    // si filtros parecen honorados, podemos permitir más porque early-stop corta
    private static final int MAX_PAGES_IF_FILTERS_HONORED = 5000;

    private static final String EUSKADI_BASE = "https://api.euskadi.eus";

    public record Resultado(
            int paginasProcesadas,
            int noticesLeidos,
            int noticesEnRango,
            int noticesEnriquecidos,
            int licitacionesUpsert
    ) {}

    private final EuskadiContractingNoticesClient noticesClient;
    private final EuskadiEnrichmentClient enrichmentClient;
    private final LicitacionUpsertService upsertService;

    public ImportarEuskadiDiarioUseCase(
            EuskadiContractingNoticesClient noticesClient,
            EuskadiEnrichmentClient enrichmentClient,
            LicitacionUpsertService upsertService
    ) {
        this.noticesClient = noticesClient;
        this.enrichmentClient = enrichmentClient;
        this.upsertService = upsertService;
    }

    /**
     * Import diario:
     * - Ajusta el filtro a "ayer" con daysBack=1 (o el que pases).
     * - Por robustez, además de confiar en filtros del endpoint, filtra localmente por fecha.
     */
    public Resultado ejecutar(int daysBack) {

        int effectiveDaysBack = (daysBack <= 0) ? 1 : daysBack;

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = todayUtc.minusDays(effectiveDaysBack);
        LocalDate toDate = todayUtc.plusDays(1); // para incluir hoy si hiciera falta

        Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = toDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        int page = 1;
        int paginas = 0;
        int leidos = 0;
        int enRango = 0;
        int enriquecidos = 0;
        int upserts = 0;

        log.info("[EUSKADI] INICIO importar diario. daysBack={} (effective={}) fromDate={} toDate={} itemsOfPage={}",
                daysBack, effectiveDaysBack, fromDate, toDate, ITEMS_OF_PAGE);

        // 1) Primera página para detectar si el endpoint respeta filtros
        EuskadiContractingNoticesClient.ListResult first;
        try {
            first = noticesClient.list(page, ITEMS_OF_PAGE, from, to);
        } catch (Exception ex) {
            log.error("[EUSKADI] ERROR leyendo primera página.", ex);
            throw ex;
        }

        JsonNode firstBody = first.body();

        // totalPages a veces viene 0; si existe totalItems, lo calculamos
        int totalPages = firstBody.path("totalPages").asInt(0);
        if (totalPages <= 0) {
            int totalItems = firstBody.path("totalItems").asInt(0);
            if (totalItems > 0) {
                totalPages = (int) Math.ceil(totalItems / (double) ITEMS_OF_PAGE);
            }
        }

        boolean filtersHonored = first.filtersSeemHonored();
        int hardCap = filtersHonored ? MAX_PAGES_IF_FILTERS_HONORED : MAX_PAGES_IF_FILTERS_NOT_HONORED;

        log.info("[EUSKADI] Primera página OK. totalPages={} filtersHonored={} urlUsed={} capPages={}",
                totalPages, filtersHonored, first.urlUsed(), hardCap);

        if (totalPages <= 0) {
            log.info("[EUSKADI] No hay páginas (totalPages<=0). FIN.");
            return new Resultado(0, 0, 0, 0, 0);
        }

        int maxPagesToScan = Math.min(totalPages, hardCap);

        // 2) Recorrer páginas
        while (page <= maxPagesToScan) {

            EuskadiContractingNoticesClient.ListResult lr;
            try {
                lr = (page == 1) ? first : noticesClient.list(page, ITEMS_OF_PAGE, from, to);
            } catch (Exception ex) {
                log.error("[EUSKADI] ERROR leyendo página {}. Se corta importación (resultado parcial). Err={}",
                        page, ex.toString(), ex);
                break;
            }

            JsonNode response = lr.body();
            JsonNode items = response.path("items");
            paginas++;

            if (!items.isArray() || items.isEmpty()) {
                log.info("[EUSKADI] Página {} sin items. Corto.", page);
                break;
            }

            // early-stop SOLO si filtros parecen honorados
            LocalDate minRelevantDateOnPage = null;

            for (JsonNode item : items) {
                leidos++;

                LocalDate firstPub = EuskadiJsonExtractors.firstPublicationDate(item);
                LocalDate lastPub = EuskadiJsonExtractors.lastPublicationDate(item);

                // filtrado local: aceptamos si first o last cae dentro del rango
                boolean inRange = isInRange(firstPub, fromDate, toDate) || isInRange(lastPub, fromDate, toDate);
                if (!inRange) {
                    if (filtersHonored) {
                        LocalDate ref = (lastPub != null) ? lastPub : firstPub;
                        if (ref != null) {
                            if (minRelevantDateOnPage == null || ref.isBefore(minRelevantDateOnPage)) {
                                minRelevantDateOnPage = ref;
                            }
                        }
                    }
                    continue;
                }

                enRango++;

                try {
                    Licitacion licitacion = mapBase(item);

                    // Enriquecimiento: contracts
                    String contractsHref = item.path("_links").path("contracts").path("href").asText(null);

                    JsonNode contracts = null;
                    if (contractsHref != null && !contractsHref.isBlank()) {
                        String url = normalizeUrl(contractsHref);
                        try {
                            contracts = enrichmentClient.fetchContractsByHref(url);
                        } catch (Exception ex) {
                            log.warn("[EUSKADI] Enrichment contracts falló. id={} expediente={} href={} err={}",
                                    safeId(item), licitacion.getExpediente(), url, ex.toString());
                        }
                    }

                    boolean enriched = false;

                    if (contracts != null && !contracts.isNull()) {
                        applyEnrichment(licitacion, contracts);
                        enriched = true;
                    } else {
                        // Fallback: detalle del notice (¡ojo! id a veces es texto)
                        long id = safeIdLong(item);
                        if (id > 0) {
                            try {
                                // Pasamos URL ABSOLUTA (porque EuskadiApiClient.getJson no acepta relativa)
                                JsonNode detail = enrichmentClient.fetchNoticeDetail(id);
                                if (detail != null && !detail.isNull()) {
                                    applyEnrichment(licitacion, detail);
                                    enriched = true;
                                }
                            } catch (Exception ex) {
                                log.warn("[EUSKADI] Fallback detail falló. id={} expediente={} err={}",
                                        safeId(item), licitacion.getExpediente(), ex.toString());
                            }
                        }
                    }

                    if (enriched) enriquecidos++;

                    upsertService.upsert(licitacion);
                    upserts++;

                } catch (Exception ex) {
                    log.error("[EUSKADI] Error procesando item (se salta). page={} leidos={} id={}",
                            page, leidos, safeId(item), ex);
                }

                if (leidos % 200 == 0) {
                    log.info("[EUSKADI] Progreso: page={}/{}, leidos={}, enRango={}, enriquecidos={}, upserts={}",
                            page, maxPagesToScan, leidos, enRango, enriquecidos, upserts);
                }
            }

            // EARLY STOP solo si filtros/orden parecen funcionar
            if (filtersHonored && minRelevantDateOnPage != null && minRelevantDateOnPage.isBefore(fromDate)) {
                log.info("[EUSKADI] Early stop: minRelevantDateOnPage={} < fromDate={} (filtersHonored=true). Corto.",
                        minRelevantDateOnPage, fromDate);
                break;
            }

            page++;
        }

        log.info("[EUSKADI] FIN. paginas={}, leidos={}, enRango={}, enriquecidos={}, upserts={}",
                paginas, leidos, enRango, enriquecidos, upserts);

        return new Resultado(paginas, leidos, enRango, enriquecidos, upserts);
    }

    /* =========================
       MAPEOS
       ========================= */

    private Licitacion mapBase(JsonNode item) {
        Licitacion l = new Licitacion();

        // en Euskadi suele venir en "code"
        l.setExpediente(EuskadiJsonExtractors.text(item, "code"));

        l.setTitulo(EuskadiJsonExtractors.titulo(item));

        // algunas respuestas usan mainEntityOfPage; otras "link"
        String urlPublica = EuskadiJsonExtractors.text(item, "mainEntityOfPage");
        if (urlPublica == null) urlPublica = item.path("link").asText(null);
        l.setUrlPublica(urlPublica);

        String tipo = item.path("contractType").path("name").asText(null);
        l.setTipoContrato(mapTipoContrato(tipo));

        l.setProcedimiento(item.path("contractProcedureType").path("name").asText(null));

        FechasProcedimiento fechas = new FechasProcedimiento();
        fechas.setFechaPublicacion(EuskadiJsonExtractors.firstPublicationDate(item));
        fechas.setFechaLimitePresentacion(EuskadiJsonExtractors.deadlineDate(item));
        l.setFechas(fechas);

        if (item.hasNonNull("budgetWithoutVAT") && item.get("budgetWithoutVAT").isNumber()) {
            BigDecimal importe = item.get("budgetWithoutVAT").decimalValue();
            Money m = new Money();
            m.setAmount(importe);
            m.setCurrency("EUR");
            m.setIncluyeIVA(false);

            l.setPresupuestoBase(m);
            l.setMoneda("EUR");
        }

        JsonNode ca = item.path("contractingAuthority");
        if (!ca.isMissingNode() && !ca.isNull()) {
            OrganismoContratacion org = new OrganismoContratacion();
            org.setNombre(ca.path("name").asText(null));
            org.setNif(ca.path("identificationNumber").asText(null));
            org.setDir3(ca.has("id") ? ca.get("id").asText() : null);
            l.setOrganismo(org);
        }

        l.setSra(item.has("sara") ? item.get("sara").asBoolean() : null);

        l.setLastEventEntryId("EUSKADI:" + safeId(item));

        LocalDate last = EuskadiJsonExtractors.lastPublicationDate(item);
        if (last != null) {
            l.setLastEventUpdatedAt(last.atStartOfDay().toInstant(ZoneOffset.UTC));
        }

        return l;
    }

    private void applyEnrichment(Licitacion lic, JsonNode node) {

        if (lic.getFechas() == null) lic.setFechas(new FechasProcedimiento());
        if (lic.getFechas().getFechaLimitePresentacion() == null) {
            LocalDate d = EuskadiJsonExtractors.deadlineDate(node);
            if (d != null) lic.getFechas().setFechaLimitePresentacion(d);
        }

        Set<String> cpvCodes = EuskadiJsonExtractors.extractCpvCodes(node);
        for (String code : cpvCodes) {
            Cpv cpv = new Cpv();
            cpv.setCodigo(code);
            lic.getCpvs().add(cpv);
        }
    }

    private TipoContrato mapTipoContrato(String name) {
        if (name == null) return null;
        String n = name.toLowerCase();
        if (n.contains("obra")) return TipoContrato.OBRAS;
        if (n.contains("sumin")) return TipoContrato.SUMINISTROS;
        if (n.contains("serv")) return TipoContrato.SERVICIOS;
        return null;
    }

    private boolean isInRange(LocalDate d, LocalDate from, LocalDate to) {
        if (d == null) return false;
        return (!d.isBefore(from)) && (!d.isAfter(to));
    }

    /* =========================
       HELPERS
       ========================= */

    private String normalizeUrl(String href) {
        if (href == null) return null;
        String h = href.trim();
        if (h.startsWith("http://") || h.startsWith("https://")) return h;
        if (!h.startsWith("/")) h = "/" + h;
        return EUSKADI_BASE + h;
    }

    private String safeId(JsonNode item) {
        try {
            JsonNode id = item.get("id");
            if (id == null || id.isNull()) return "?";
            return id.asText("?");
        } catch (Exception e) {
            return "?";
        }
    }

    private long safeIdLong(JsonNode item) {
        try {
            String s = safeId(item);
            if (s == null) return 0L;
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0L;
        }
    }
}

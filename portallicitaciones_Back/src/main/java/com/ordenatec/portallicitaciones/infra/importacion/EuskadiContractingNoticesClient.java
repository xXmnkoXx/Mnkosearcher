package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente para listar notices de Euskadi:
 * - Usa el endpoint oficial:
 *   /procurements/contracting-notices?publication-date.gt=YYYY-MM-DD&orderBy=lastPublicationDate&orderType=DESC&currentPage=1&itemsOfPage=10&lang=SPANISH
 * - itemsOfPage máximo 50
 * - Incluye fallbacks porque el endpoint a veces es inconsistente
 * - Devuelve meta: url usada + si parece que los filtros se respetan
 */
@Component
public class EuskadiContractingNoticesClient {

    private static final Logger log = LoggerFactory.getLogger(EuskadiContractingNoticesClient.class);

    private static final int MAX_ITEMS_OF_REQUEST = 50;

    private static final String BASE_URL = "https://api.euskadi.eus";
    private static final String PATH = "/procurements/contracting-notices";

    // La API usa "currentPage" y "itemsOfPage". Idioma: SPANISH (según tu ejemplo).
    private static final String DEFAULT_LANG = "SPANISH";

    public record ListResult(JsonNode body, String urlUsed, boolean filtersSeemHonored) {}

    private final EuskadiApiClient apiClient;

    public EuskadiContractingNoticesClient(EuskadiApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Lista notices paginados, intentando respetar un rango de fechas (por día).
     * En la práctica, con tu caso de uso "ayer", basta con pasar fromInclusive = ayer 00:00 UTC
     * y toExclusive = hoy 00:00 UTC, aunque el endpoint oficial sólo use publication-date.gt.
     */
    public ListResult list(int page, int itemsOfPage, Instant fromInclusive, Instant toExclusive) {

        int safeItems = Math.min(Math.max(itemsOfPage, 1), MAX_ITEMS_OF_REQUEST);

        LocalDate fromDate = fromInclusive.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate toDate = toExclusive.atZone(ZoneOffset.UTC).toLocalDate();

        List<String> urls = buildCandidateUrls(page, safeItems, fromDate, toDate);

        JsonNode lastOk = null;
        String lastUrl = null;

        for (String url : urls) {
            try {
                log.info("[EUSKADI] ContractingNotices LIST -> currentPage={} itemsOfPage={} from={} to={} url={}",
                        page, safeItems, fromDate, toDate, url);

                JsonNode body = apiClient.getJson(url);
                lastOk = body;
                lastUrl = url;

                boolean honored = seemsHonoringFilter(body, fromDate, toDate);
                if (!honored) {
                    log.warn("[EUSKADI] WARNING: el endpoint parece IGNORAR filtros de fecha. from={} to={} currentPage={} itemsOfPage={}",
                            fromDate, toDate, page, safeItems);
                    // probamos siguiente variante
                    continue;
                }

                // si parece que funciona, devolvemos
                return new ListResult(body, url, true);

            } catch (Exception ex) {
                log.warn("[EUSKADI] Fallo con variante URL (se prueba otra). url={} err={}", url, ex.toString());
            }
        }

        // Si ninguna variante “parece” funcionar, devolvemos la última respuesta OK que obtuvimos
        if (lastOk != null) {
            return new ListResult(lastOk, lastUrl, false);
        }

        throw new RuntimeException("No se pudo llamar a Euskadi ContractingNotices con ninguna variante de parámetros.");
    }

    /**
     * Construye variantes de URL:
     * 1) ✅ La oficial (la de tu ejemplo): publication-date.gt + currentPage + itemsOfPage + orderBy + orderType + lang
     * 2) Variante añadiendo publication-date.lt (por si la aceptase)
     * 3) Fallbacks antiguos (por si tuvieras comportamiento previo)
     */
    private List<String> buildCandidateUrls(int page, int itemsOfPage, LocalDate fromDate, LocalDate toDate) {

        String baseOfficial = BASE_URL + PATH
                + "?publication-date.gt=" + fromDate
                + "&orderBy=lastPublicationDate"
                + "&orderType=DESC"
                + "&currentPage=" + page
                + "&itemsOfPage=" + itemsOfPage
                + "&lang=" + DEFAULT_LANG;

        List<String> urls = new ArrayList<>();

        // 1) OFICIAL (tu ejemplo)
        urls.add(baseOfficial);

        // 2) Por si aceptase un límite superior (no siempre documentado)
        urls.add(BASE_URL + PATH
                + "?publication-date.gt=" + fromDate
                + "&publication-date.lt=" + toDate
                + "&orderBy=lastPublicationDate"
                + "&orderType=DESC"
                + "&currentPage=" + page
                + "&itemsOfPage=" + itemsOfPage
                + "&lang=" + DEFAULT_LANG);

        // 3) Fallbacks legacy (por si la API cambiase/rompiera lo oficial)
        String baseLegacy = BASE_URL + PATH
                + "?page=" + page
                + "&itemsOfPage=" + itemsOfPage;

        urls.add(baseLegacy
                + "&publicationDateGreaterThan=" + fromDate
                + "&publicationDateLowerThan=" + toDate
                + "&orderBy=lastPublicationDate&order=desc");

        urls.add(baseLegacy
                + "&firstPublicationDateGreaterThan=" + fromDate
                + "&firstPublicationDateLowerThan=" + toDate
                + "&lastPublicationDateGreaterThan=" + fromDate
                + "&lastPublicationDateLowerThan=" + toDate
                + "&orderBy=lastPublicationDate&order=desc");

        urls.add(baseLegacy
                + "&firstPublicationDateGreaterThanOrEqual=" + fromDate
                + "&firstPublicationDateLowerThanOrEqual=" + toDate
                + "&lastPublicationDateGreaterThanOrEqual=" + fromDate
                + "&lastPublicationDateLowerThanOrEqual=" + toDate
                + "&orderBy=lastPublicationDate&order=desc");

        return urls;
    }

    /**
     * Heurística:
     * Si al menos 1 item cae dentro del rango (firstPublicationDate o lastPublicationDate),
     * asumimos que el filtro “algo” está funcionando.
     */
    private boolean seemsHonoringFilter(JsonNode body, LocalDate fromDate, LocalDate toDate) {
        JsonNode items = body.path("items");
        if (!items.isArray() || items.isEmpty()) {
            // si no hay items no sabemos; no lo marcamos como ignorado
            return true;
        }

        int checked = 0;
        for (JsonNode it : items) {
            if (checked++ >= 30) break;

            LocalDate first = EuskadiJsonExtractors.firstPublicationDate(it);
            LocalDate last = EuskadiJsonExtractors.lastPublicationDate(it);

            if (isInRange(first, fromDate, toDate) || isInRange(last, fromDate, toDate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInRange(LocalDate d, LocalDate from, LocalDate to) {
        if (d == null) return false;
        // rango [from, to] con to inclusivo a nivel LocalDate (para “días”)
        return (!d.isBefore(from)) && (!d.isAfter(to));
    }
}

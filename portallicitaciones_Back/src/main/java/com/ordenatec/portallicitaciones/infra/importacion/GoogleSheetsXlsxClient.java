package com.ordenatec.portallicitaciones.infra.importacion;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descarga un Google Spreadsheet como XLSX a partir de un link compartido.
 *
 * Link ejemplo:
 * https://docs.google.com/spreadsheets/d/<SHEET_ID>/edit?usp=sharing
 *
 * Lo convierte a:
 * https://docs.google.com/spreadsheets/d/<SHEET_ID>/export?format=xlsx
 */
@Component
public class GoogleSheetsXlsxClient {

    private static final Pattern SHEET_ID_PATTERN =
            Pattern.compile("https?://docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9-_]+)(/.*)?");

    private final RestTemplate restTemplate;

    public GoogleSheetsXlsxClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] downloadXlsxFromShareUrl(String shareUrl) {
        String sheetId = extractSheetId(shareUrl);
        String exportUrl = buildExportUrl(sheetId);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/octet-stream"
        ));
        // A veces Google se pone tonto sin User-Agent
        headers.set("User-Agent", "Mozilla/5.0");

        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<byte[]> res = restTemplate.exchange(
                URI.create(exportUrl),
                HttpMethod.GET,
                req,
                byte[].class
        );

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null || res.getBody().length == 0) {
            throw new IllegalStateException("No se pudo descargar XLSX desde Google Sheets. HTTP=" + res.getStatusCode());
        }

        return res.getBody();
    }

    private String extractSheetId(String shareUrl) {
        if (shareUrl == null || shareUrl.isBlank()) {
            throw new IllegalArgumentException("shareUrl vacío");
        }
        Matcher m = SHEET_ID_PATTERN.matcher(shareUrl.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("URL no válida de Google Sheets: " + shareUrl);
        }
        return m.group(1);
    }

    private String buildExportUrl(String sheetId) {
        // gid opcional (por defecto la primera pestaña). En tu Excel la pestaña importante es "Licitaciones",
        // luego la leeremos por nombre con Apache POI, así que no dependemos del gid.
        return "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=xlsx";
    }
}

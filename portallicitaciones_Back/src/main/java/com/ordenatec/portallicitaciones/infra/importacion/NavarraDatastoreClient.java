package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP para descargar el dump JSON del datastore de Navarra.
 */
@Component
public class NavarraDatastoreClient {

    /**
     * URL del dump del datastore (la que has validado).
     * Nota: viene con BOM cuando bom=True.
     */
    private static final String SOURCE_URL =
            "https://datosabiertos.navarra.es/es/datastore/dump/dda1af7c-0dcd-4992-9852-ded6b1e7625d?format=json&bom=True";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public JsonNode descargarDump() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SOURCE_URL))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Ordenatec-PortalLicitaciones/1.0")
                    .GET()
                    .build();

            // 👇 importante: leer como bytes para manejar BOM bien
            HttpResponse<byte[]> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode()
                        + " al descargar dump Navarra");
            }

            String body = new String(response.body(), StandardCharsets.UTF_8);

            // ✅ Eliminar BOM UTF-8 si viene al inicio
            if (!body.isEmpty() && body.charAt(0) == '\uFEFF') {
                body = body.substring(1);
            }

            return objectMapper.readTree(body);

        } catch (Exception e) {
            throw new RuntimeException("Error descargando dump Navarra", e);
        }
    }

    public String getSourceUrl() {
        return SOURCE_URL;
    }
}

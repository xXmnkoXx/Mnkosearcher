package com.ordenatec.portallicitaciones.infra.detalle;

import com.ordenatec.portallicitaciones.infra.http.DetalleHttpClient;
import org.springframework.stereotype.Component;

/**
 * Extractor por defecto: no parsea nada.
 * Sirve para que compile y para poder depurar qué llega.
 */
@Component
public class FallbackDetalleExtractor implements DetalleExtractor {

    @Override
    public DetalleExtraido extraer(DetalleHttpClient.FetchResult fetch) {
        DetalleExtraido out = new DetalleExtraido();
        if (fetch != null) {
            out.setUrlFinal(fetch.finalUrl());
            out.setContentType(fetch.contentType());
        }
        return out;
    }
}

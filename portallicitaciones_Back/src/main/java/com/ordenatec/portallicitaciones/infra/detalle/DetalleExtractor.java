package com.ordenatec.portallicitaciones.infra.detalle;

import com.ordenatec.portallicitaciones.infra.http.DetalleHttpClient;

public interface DetalleExtractor {
    DetalleExtraido extraer(DetalleHttpClient.FetchResult fetch);
}

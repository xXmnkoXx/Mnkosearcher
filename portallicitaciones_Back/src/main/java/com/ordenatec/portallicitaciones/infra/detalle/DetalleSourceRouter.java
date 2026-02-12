package com.ordenatec.portallicitaciones.infra.detalle;

import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Router: dado un url_publica, decide qué extractor usar.
 *
 * ⚠️ Versión temporal:
 * - SOLO usa FallbackDetalleExtractor
 * - El resto de extractores se activarán más adelante
 */
@Component
public class DetalleSourceRouter {

    private final DetalleExtractor fallbackExtractor;

    public DetalleSourceRouter(FallbackDetalleExtractor fallbackExtractor) {
        this.fallbackExtractor = fallbackExtractor;
    }

    /**
     * Por ahora SIEMPRE devolvemos fallback para evitar errores
     */
    public DetalleExtractor resolver(String urlPublica) {
        return fallbackExtractor;
    }

    /**
     * Helper conservado para cuando se activen los extractores reales
     */
    @SuppressWarnings("unused")
    private static String host(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return URI.create(url.trim()).getHost();
        } catch (Exception ignore) {
            return null;
        }
    }
}

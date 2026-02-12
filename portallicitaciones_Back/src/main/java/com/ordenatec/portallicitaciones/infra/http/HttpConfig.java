package com.ordenatec.portallicitaciones.infra.http;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.time.Duration;

/**
 * RestTemplate con SSL opcional "inseguro" para DEV.
 * Activar con: -Dportal.http.insecureSsl=true
 */
@Configuration
public class HttpConfig {

    private static final Logger log = LoggerFactory.getLogger(HttpConfig.class);

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${portal.http.insecureSsl:false}") boolean insecureSsl,
            @Value("${portal.http.timeoutMs:30000}") int timeoutMs
    ) throws Exception {

        builder = builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs));

        if (!insecureSsl) {
            return builder.build();
        }

        // DEV ONLY: confiar en TODO (certificado + hostname)
        TrustStrategy trustAll = (chain, authType) -> true;

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, trustAll)
                .build();

        SSLConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        var connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .evictExpiredConnections()
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // timeouts a nivel factory también (por si acaso)
        factory.setConnectTimeout(timeoutMs);
        factory.setConnectionRequestTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        log.warn("[HTTP] insecureSsl=true (NO USAR EN PRODUCCIÓN).");

        return builder.requestFactory(() -> factory).build();
    }
}

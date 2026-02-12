package com.ordenatec.portallicitaciones.infra.importacion;

import com.ordenatec.portallicitaciones.domain.port.AtomSourceResolver;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;

/**
 * Resuelve la URL del ZIP mensual de la sindicación 643 (PLACSP).
 *
 * Formato:
 *  https://contrataciondelsectorpublico.gob.es/sindicacion/sindicacion_643/
 *      licitacionesPerfilesContratanteCompleto3_YYYYMM.zip
 */
@Component
public class PlcspZipSourceResolver implements AtomSourceResolver {

    private static final String BASE =
            "https://contrataciondelsectorpublico.gob.es/sindicacion/sindicacion_643/";
    private static final String FILE_PREFIX = "licitacionesPerfilesContratanteCompleto3_";
    private static final String FILE_SUFFIX = ".zip";

    @Override
    public URI resolverZipMasReciente() {
        LocalDate now = LocalDate.now();
        String yyyymm = String.format("%d%02d", now.getYear(), now.getMonthValue());
        return URI.create(BASE + FILE_PREFIX + yyyymm + FILE_SUFFIX);
    }

    /**
     * Permite construir explícitamente la URL para un mes concreto.
     * Útil para reimportar meses anteriores.
     */
    public URI resolverZipPorMes(int year, int month1to12) {
        if (month1to12 < 1 || month1to12 > 12) {
            throw new IllegalArgumentException("month1to12 debe estar entre 1 y 12");
        }
        String yyyymm = String.format("%d%02d", year, month1to12);
        return URI.create(BASE + FILE_PREFIX + yyyymm + FILE_SUFFIX);
    }
}

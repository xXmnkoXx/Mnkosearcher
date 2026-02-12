package com.ordenatec.portallicitaciones.infra.importacion;

import com.ordenatec.portallicitaciones.domain.port.AtomSourceResolver;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;

@Component
public class HaciendaAtomSourceResolver implements AtomSourceResolver {

    @Override
    public URI resolverZipMasReciente() {
        LocalDate now = LocalDate.now();

        String yyyymm = String.format("%d%02d", now.getYear(), now.getMonthValue());

        String url = "https://contrataciondelsectorpublico.gob.es/sindicacion/sindicacion_1044/"
                + "PlataformasAgregadasSinMenores_" + yyyymm + ".zip";

        return URI.create(url);
    }
}

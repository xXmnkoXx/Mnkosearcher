package com.ordenatec.portallicitaciones.api.dto;

import com.ordenatec.portallicitaciones.domain.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class LicitacionDetalleDtoMapper {

    public static LicitacionDetalleDto map(Licitacion l) {

        FechasProcedimiento f = l.getFechas();

        LocalDate fechaPublicacion =
                f != null ? f.getFechaPublicacion() : null;

        // ✅ TU MODELO REAL: getFechaLimitePresentacion()
        LocalDate fechaLimite =
                f != null ? f.getFechaLimitePresentacion() : null;

        return new LicitacionDetalleDto(
                l.getId(),
                l.getExpediente(),
                l.getReferenciaPlataforma(),
                l.getTitulo(),
                l.getUrlPublica(),

                nombreOrganismo(l.getOrganismo()),
                l.getEstado() != null ? l.getEstado().name() : null,

                fechaPublicacion,
                fechaLimite,

                amount(l.getPresupuestoBase()),
                amount(l.getValorEstimado()),
                l.getMoneda(),

                l.getLugarEjecucion(),
                l.getEntidad(),
                l.getOrgano(),

                l.getCpvs().stream()
                        .map(Cpv::getCodigo)
                        .collect(Collectors.toList()),

                l.getDocumentos().stream()
                        .map(d -> new LicitacionDetalleDto.DocumentoDto(
                                d.getTipo(),
                                // ✅ TU MODELO REAL: getTitulo()
                                d.getTitulo(),
                                d.getUrl()
                        ))
                        .collect(Collectors.toList())
        );
    }

    private static String nombreOrganismo(OrganismoContratacion o) {
        return o != null ? o.getNombre() : null;
    }

    private static BigDecimal amount(Money m) {
        return m != null ? m.getAmount() : null;
    }
}

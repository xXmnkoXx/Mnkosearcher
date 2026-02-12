package com.ordenatec.portallicitaciones.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LicitacionUnificadaDto(
        String fuente, // "NACIONAL" | "CATALUNYA"
        String idExterno,
        String expediente,
        String titulo,
        String objeto,
        String organoNombre,
        String organoDir3,
        String departamento,
        String tipoContrato,
        String procedimiento,
        String estadoFase,
        String cpvPrincipal,
        String cpvs,
        String nuts,
        String lugarEjecucion,
        BigDecimal valorEstimadoSinIva,
        BigDecimal presupuestoSinIva,
        BigDecimal presupuestoConIva,
        BigDecimal importeAdjudicacionSinIva,
        BigDecimal importeAdjudicacionConIva,
        String adjudicatarioNombre,
        String adjudicatarioId,
        Integer numOfertas,
        LocalDateTime fechaPublicacion,
        LocalDateTime fechaLimitePresentacion,
        LocalDateTime fechaAdjudicacion,
        String duracion,
        String enlace
) {}

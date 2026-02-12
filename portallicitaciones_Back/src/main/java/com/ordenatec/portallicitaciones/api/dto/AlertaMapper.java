package com.ordenatec.portallicitaciones.api.dto;

import com.ordenatec.portallicitaciones.infra.persistence.entity.AlertaEntity;

import java.time.LocalTime;

public class AlertaMapper {

    private AlertaMapper() {}

    public static AlertaResponse toResponse(AlertaEntity e) {
        AlertaResponse r = new AlertaResponse();

        r.setIdAlerta(e.getIdAlerta());
        r.setIdUsuario(e.getIdUsuario());

        r.setNombre(e.getNombre());

        r.setCpvs(e.getCpvs());
        r.setDescripcion(e.getDescripcion());
        r.setTipo(e.getTipo());
        r.setImporteMin(e.getImporteMin());
        r.setImporteMax(e.getImporteMax());
        r.setActiva(e.getActiva());
        r.setFechaCreacion(e.getFechaCreacion());

        r.setPalabrasClave(e.getPalabrasClave());
        r.setDescripcionActividad(e.getDescripcionActividad());
        r.setTiposContratos(e.getTiposContratos());
        r.setContratosMenores(e.getContratosMenores());
        r.setLugares(e.getLugares());
        r.setOrganosContratacion(e.getOrganosContratacion());

        r.setFrecuenciaEnvio(e.getFrecuenciaEnvio());
        r.setHoraEnvio(e.getHoraEnvio() != null ? e.getHoraEnvio().toString() : null);
        r.setDiaSemana(e.getDiaSemana());
        r.setNotificarCambios(e.getNotificarCambios());

        r.setEmail1(e.getEmail1());
        r.setEmail2(e.getEmail2());
        r.setEmail3(e.getEmail3());

        return r;
    }

    /** Copia campos del request dentro de una entity existente (para create o update). */
    public static void applyRequest(AlertaRequest req, AlertaEntity e) {
        e.setNombre(req.getNombre());

        e.setCpvs(req.getCpvs());
        e.setDescripcion(req.getDescripcion());
        e.setTipo(req.getTipo());
        e.setImporteMin(req.getImporteMin());
        e.setImporteMax(req.getImporteMax());
        e.setActiva(req.getActiva() != null ? req.getActiva() : true);

        e.setPalabrasClave(req.getPalabrasClave());
        e.setDescripcionActividad(req.getDescripcionActividad());
        e.setTiposContratos(req.getTiposContratos());
        e.setContratosMenores(req.getContratosMenores() != null ? req.getContratosMenores() : false);
        e.setLugares(req.getLugares());
        e.setOrganosContratacion(req.getOrganosContratacion());

        e.setFrecuenciaEnvio(req.getFrecuenciaEnvio());
        e.setHoraEnvio(parseHora(req.getHoraEnvio()));
        e.setDiaSemana(req.getDiaSemana());
        e.setNotificarCambios(req.getNotificarCambios() != null ? req.getNotificarCambios() : true);

        e.setEmail1(req.getEmail1());
        e.setEmail2(req.getEmail2());
        e.setEmail3(req.getEmail3());
    }

    private static LocalTime parseHora(String hora) {
        if (hora == null || hora.isBlank()) return null;
        return LocalTime.parse(hora.trim()); // "10:00" o "10:00:00"
    }
}

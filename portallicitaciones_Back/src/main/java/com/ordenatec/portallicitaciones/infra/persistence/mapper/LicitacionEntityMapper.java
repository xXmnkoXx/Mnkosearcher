package com.ordenatec.portallicitaciones.infra.persistence.mapper;

import com.ordenatec.portallicitaciones.domain.model.FechasProcedimiento;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;

public class LicitacionEntityMapper {

    private LicitacionEntityMapper() {
    }

    // =========================================================
    // DOMAIN -> ENTITY
    // =========================================================
    public static LicitacionEntity toEntity(Licitacion domain) {
        if (domain == null) return null;

        LicitacionEntity e = new LicitacionEntity();

        e.setUuid(domain.getId());
        e.setExpediente(domain.getExpediente());
        e.setTitulo(domain.getTitulo());
        e.setUrlPublica(domain.getUrlPublica());

        // -------- FECHAS --------
        if (domain.getFechas() != null) {
            FechasProcedimiento f = domain.getFechas();
            e.setFechaPublicacion(f.getFechaPublicacion());

            // ✅ AQUÍ ESTABA EL FALLO: ahora sí se persiste
            e.setFechaLimitePresentacion(f.getFechaLimitePresentacion());
        }

        // -------- TEXTO / METADATOS --------
        e.setEntidad(domain.getEntidad());
        e.setProcedimiento(domain.getProcedimiento());
        e.setCriterioAdjudicacion(domain.getCriterioAdjudicacion());
        e.setCodigoNuts(domain.getCodigoNuts());
        e.setLugarEjecucion(domain.getLugarEjecucion());
        e.setMotivo(domain.getMotivo());
        e.setPlazoEjecucion(domain.getPlazoEjecucion());

        if (domain.getTipoContrato() != null) {
            e.setTipoContratoTexto(domain.getTipoContrato().name());
        }

        if (domain.getEstado() != null) {
            e.setEstadoTexto(domain.getEstado().name());
        }

        // -------- IMPORTES --------
        if (domain.getPresupuestoBase() != null) {
            e.setPrecioLicitacion(domain.getPresupuestoBase().getAmount());
        }

        if (domain.getValorEstimado() != null) {
            e.setValorEstimado(domain.getValorEstimado().getAmount());
        }

        e.setMoneda(domain.getMoneda());

        e.setFechaUltimaActualizacion(domain.getLastEventUpdatedAt());

        return e;
    }

    // =========================================================
    // ENTITY -> DOMAIN
    // =========================================================
    public static Licitacion toDomain(LicitacionEntity e) {
        if (e == null) return null;

        Licitacion d = new Licitacion();

        d.setId(e.getUuid());
        d.setExpediente(e.getExpediente());
        d.setTitulo(e.getTitulo());
        d.setUrlPublica(e.getUrlPublica());

        // -------- FECHAS --------
        FechasProcedimiento fechas = new FechasProcedimiento();
        fechas.setFechaPublicacion(e.getFechaPublicacion());

        // ✅ AQUÍ TAMBIÉN: vuelve al dominio
        fechas.setFechaLimitePresentacion(e.getFechaLimitePresentacion());

        d.setFechas(fechas);

        // -------- TEXTO / METADATOS --------
        d.setEntidad(e.getEntidad());
        d.setProcedimiento(e.getProcedimiento());
        d.setCriterioAdjudicacion(e.getCriterioAdjudicacion());
        d.setCodigoNuts(e.getCodigoNuts());
        d.setLugarEjecucion(e.getLugarEjecucion());
        d.setMotivo(e.getMotivo());
        d.setPlazoEjecucion(e.getPlazoEjecucion());

        d.setMoneda(e.getMoneda());
        d.setLastEventUpdatedAt(e.getFechaUltimaActualizacion());

        // enums por texto
        if (e.getTipoContratoTexto() != null) {
            d.setTipoContrato(
                    com.ordenatec.portallicitaciones.domain.enums.TipoContrato
                            .valueOf(e.getTipoContratoTexto())
            );
        }

        if (e.getEstadoTexto() != null) {
            d.setEstado(
                    com.ordenatec.portallicitaciones.domain.enums.EstadoLicitacion
                            .valueOf(e.getEstadoTexto())
            );
        }

        return d;
    }
}

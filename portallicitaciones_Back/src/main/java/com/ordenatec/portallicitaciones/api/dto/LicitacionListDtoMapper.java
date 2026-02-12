package com.ordenatec.portallicitaciones.api.dto;

import com.ordenatec.portallicitaciones.infra.persistence.entity.CpvEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.OrganismoEntity;

import java.util.Comparator;
import java.util.List;

public class LicitacionListDtoMapper {

    public static LicitacionListDto map(LicitacionEntity e) {
        LicitacionListDto dto = new LicitacionListDto();

        dto.uuid = e.getUuid();
        dto.expediente = e.getExpediente();
        dto.titulo = e.getTitulo();

        dto.urlPublica = e.getUrlPublica();
        dto.estadoTexto = e.getEstadoTexto();

        dto.fechaPublicacion = e.getFechaPublicacion();
        dto.fechaLimitePresentacion = e.getFechaLimitePresentacion();

        // ✅ FK organismo_id -> nombre + nif
        OrganismoEntity org = e.getOrganismo();
        if (org != null) {
            dto.organismoId = org.getId();
            dto.organismo = org.getNombre();
            dto.organismoNif = org.getNif();
        } else {
            dto.organismoId = null;
            dto.organismo = null;
            dto.organismoNif = null;
        }

        // compatibilidad
        dto.entidad = e.getEntidad();

        dto.procedimiento = e.getProcedimiento();
        dto.tipoContratoTexto = e.getTipoContratoTexto();

        dto.precioLicitacion = e.getPrecioLicitacion();
        dto.valorEstimado = e.getValorEstimado();
        dto.moneda = e.getMoneda();

        if (e.getCpvs() != null) {
            dto.cpvs = e.getCpvs().stream()
                    .map(CpvEntity::getCodigo)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } else {
            dto.cpvs = List.of();
        }

        return dto;
    }
}

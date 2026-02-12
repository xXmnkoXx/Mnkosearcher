package com.ordenatec.portallicitaciones.application.service;

import com.ordenatec.portallicitaciones.api.dto.LicitacionFullDto;
import com.ordenatec.portallicitaciones.api.dto.LicitacionFullDtoMapper;
import com.ordenatec.portallicitaciones.api.dto.LicitacionListDto;
import com.ordenatec.portallicitaciones.api.dto.LicitacionListDtoMapper;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.domain.port.LicitacionRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;

// ⬇️ AJUSTA estos 2 si en tu proyecto tienen otro nombre/paquete
import com.ordenatec.portallicitaciones.infra.persistence.entity.OrganismoEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.OrganismoJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LicitacionService {

    private final LicitacionRepository licitacionRepository;       // dominio (listado viejo)
    private final LicitacionJpaRepository licitacionJpaRepository; // JPA (detalle full + listados DTO)

    // ✅ NUEVO: para resolver organismoId -> (nombre, nif) en Java (no SQL)
    private final OrganismoJpaRepository organismoJpaRepository;

    public LicitacionService(
            LicitacionRepository licitacionRepository,
            LicitacionJpaRepository licitacionJpaRepository,
            OrganismoJpaRepository organismoJpaRepository
    ) {
        this.licitacionRepository = licitacionRepository;
        this.licitacionJpaRepository = licitacionJpaRepository;
        this.organismoJpaRepository = organismoJpaRepository;
    }

    // -----------------------------------------
    // ✅ LISTADO "VIEJO" (NO ROMPER FRONT): /api/licitaciones
    // -----------------------------------------
    @Transactional(readOnly = true)
    public List<Licitacion> listar() {
        return licitacionRepository.findAll();
    }

    // -----------------------------------------
    // ✅ LISTADO DTO (con organismo, etc.) para endpoint NUEVO: /api/licitaciones/list
    // -----------------------------------------
    @Transactional(readOnly = true)
    public List<LicitacionListDto> listarResumen() {
        List<LicitacionListDto> dtos = licitacionJpaRepository.findAllForList().stream()
                .map(LicitacionListDtoMapper::map)
                .toList();

        // ✅ aquí resolvemos organismoId -> nombre/nif (sin SQL manual)
        enrichOrganismo(dtos);

        return dtos;
    }

    // -----------------------------------------
    // ✅ Detalle FULL para el front: /api/licitaciones/{uuid}
    // -----------------------------------------
    @Transactional(readOnly = true)
    public Optional<LicitacionFullDto> obtenerDetalleFull(UUID id) {
        return licitacionJpaRepository.findWithAllByUuid(id)
                .map(entity -> {
                    // forzar carga de colecciones si es LAZY
                    if (entity.getDocumentos() != null) entity.getDocumentos().size();
                    if (entity.getLotes() != null) entity.getLotes().size();
                    if (entity.getCpvs() != null) entity.getCpvs().size();

                    LicitacionFullDto dto = LicitacionFullDtoMapper.map(entity);

                    // ✅ Resolver organismo en el DTO full también (si tiene organismoId)
                    enrichOrganismo(dto);

                    return dto;
                });
    }

    // -----------------------------------------
    // ✅ Listado filtrado (estado + fecha + cpvs) => /api/licitaciones/publicadas
    // -----------------------------------------
    @Transactional(readOnly = true)
    public Page<LicitacionListDto> listarPublicadasFiltradas(
            String estado,
            LocalDate fromDate,
            String cpvsCsv,
            int page,
            int size
    ) {
        String estadoFinal = normalizarEstado(estado);
        if (estadoFinal == null || estadoFinal.isBlank()) {
            estadoFinal = "PUB";
        }

        LocalDate fromFinal = (fromDate == null) ? LocalDate.now() : fromDate;

        List<String> cpvList = (cpvsCsv == null || cpvsCsv.isBlank())
                ? List.of()
                : Arrays.stream(cpvsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();

        boolean cpvsEmpty = cpvList.isEmpty();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 500)
        );

        Page<LicitacionListDto> pageDtos = licitacionJpaRepository
                .buscarPublicadas(estadoFinal, fromFinal, cpvList, cpvsEmpty, pageable)
                .map(LicitacionListDtoMapper::map);

        // ✅ Enriquecer organismoId -> nombre/nif para TODOS los items del page
        enrichOrganismo(pageDtos.getContent());

        return pageDtos;
    }

    // ============================================================
    // ✅ Helpers para organismoId -> nombre/nif (SIN SQL MANUAL)
    // ============================================================

    /**
     * Enriquece una lista de LicitacionListDto con organismo / organismoNif
     * a partir de organismoId (batch, sin N+1).
     */
    private void enrichOrganismo(List<LicitacionListDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        Set<Long> ids = dtos.stream()
                .map(d -> d.organismoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return;

        Map<Long, OrganismoEntity> orgMap = organismoJpaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(OrganismoEntity::getId, o -> o));

        for (LicitacionListDto d : dtos) {
            if (d.organismoId == null) continue;

            OrganismoEntity o = orgMap.get(d.organismoId);
            if (o != null) {
                d.organismo = o.getNombre();
                d.organismoNif = o.getNif();
            }
        }
    }

    /**
     * Enriquece el DTO Full (si en tu LicitacionFullDto también tienes organismoId/organismo/organismoNif).
     * Si tu FullDto usa otros nombres, ajusta aquí.
     */
    private void enrichOrganismo(LicitacionFullDto dto) {
        if (dto == null) return;

        // ⚠️ Si tu LicitacionFullDto no tiene estos campos, comenta esto o ajusta nombres.
        try {
            // Asumimos que LicitacionFullDto tiene: organismoId, organismo, organismoNif (igual que list)
            Long orgId = (Long) dto.getClass().getField("organismoId").get(dto);
            if (orgId == null) return;

            organismoJpaRepository.findById(orgId).ifPresent(o -> {
                try {
                    dto.getClass().getField("organismo").set(dto, o.getNombre());
                    dto.getClass().getField("organismoNif").set(dto, o.getNif());
                } catch (Exception ignored) { }
            });
        } catch (Exception ignored) {
            // Si no existen esos campos en el FullDto, no hacemos nada.
        }
    }

    private String normalizarEstado(String estado) {
        if (estado == null) return null;

        String e = estado.trim().toLowerCase();

        return switch (e) {
            case "publicada", "pub" -> "PUB";
            case "resuelta", "res", "resuelto" -> "RES";
            case "evaluacion", "evaluación", "ev" -> "EV";
            case "adjudicada", "adj" -> "ADJ";
            case "previa", "pre" -> "PRE";
            case "anulada", "anul" -> "ANUL";
            default -> estado.trim();
        };
    }
}

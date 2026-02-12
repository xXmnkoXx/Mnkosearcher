package com.ordenatec.portallicitaciones.application.service;

import com.ordenatec.portallicitaciones.api.dto.AlertaMapper;
import com.ordenatec.portallicitaciones.api.dto.AlertaRequest;
import com.ordenatec.portallicitaciones.api.dto.AlertaResponse;
import com.ordenatec.portallicitaciones.infra.persistence.entity.AlertaEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.AlertaJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertaService {

    private final AlertaJpaRepository repo;

    public AlertaService(AlertaJpaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> listarMias(Integer idUsuario) {
        return repo.findByIdUsuarioOrderByIdAlertaDesc(idUsuario)
                .stream()
                .map(AlertaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertaResponse obtener(Integer idUsuario, Long idAlerta) {
        AlertaEntity e = repo.findByIdAlertaAndIdUsuario(idAlerta, idUsuario)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        return AlertaMapper.toResponse(e);
    }

    @Transactional
    public AlertaResponse crear(Integer idUsuario, AlertaRequest req) {
        AlertaEntity e = new AlertaEntity();
        e.setIdUsuario(idUsuario);
        e.setFechaCreacion(LocalDateTime.now());

        // Defaults razonables por si el front no los manda
        if (req.getActiva() == null) req.setActiva(true);
        if (req.getContratosMenores() == null) req.setContratosMenores(false);
        if (req.getNotificarCambios() == null) req.setNotificarCambios(true);

        AlertaMapper.applyRequest(req, e);

        AlertaEntity saved = repo.save(e);
        return AlertaMapper.toResponse(saved);
    }

    @Transactional
    public AlertaResponse actualizar(Integer idUsuario, Long idAlerta, AlertaRequest req) {
        AlertaEntity e = repo.findByIdAlertaAndIdUsuario(idAlerta, idUsuario)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        // Si no vienen, conserva lo que ya hay
        if (req.getActiva() == null) req.setActiva(e.getActiva());
        if (req.getContratosMenores() == null) req.setContratosMenores(e.getContratosMenores());
        if (req.getNotificarCambios() == null) req.setNotificarCambios(e.getNotificarCambios());

        AlertaMapper.applyRequest(req, e);

        AlertaEntity saved = repo.save(e);
        return AlertaMapper.toResponse(saved);
    }

    @Transactional
    public void eliminar(Integer idUsuario, Long idAlerta) {
        AlertaEntity e = repo.findByIdAlertaAndIdUsuario(idAlerta, idUsuario)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        repo.delete(e);
    }
}

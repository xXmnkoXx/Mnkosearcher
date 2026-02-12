package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.AlertaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaJpaRepository extends JpaRepository<AlertaEntity, Long> {

    // Listar alertas de un usuario
    List<AlertaEntity> findByIdUsuarioOrderByIdAlertaDesc(Integer idUsuario);

    // Buscar una alerta asegurando que pertenece al usuario
    Optional<AlertaEntity> findByIdAlertaAndIdUsuario(Long idAlerta, Integer idUsuario);
}

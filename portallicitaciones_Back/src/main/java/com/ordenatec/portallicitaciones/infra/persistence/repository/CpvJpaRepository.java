package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.CpvEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CpvJpaRepository extends JpaRepository<CpvEntity, Long> {

    Optional<CpvEntity> findByCodigo(String codigo);
}

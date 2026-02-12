package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.EmpresaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaJpaRepository extends JpaRepository<EmpresaEntity, Integer> {
}

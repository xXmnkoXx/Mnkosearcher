package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.OrganismoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganismoJpaRepository extends JpaRepository<OrganismoEntity, Long> {

    Optional<OrganismoEntity> findByCodigo(String codigo);
}

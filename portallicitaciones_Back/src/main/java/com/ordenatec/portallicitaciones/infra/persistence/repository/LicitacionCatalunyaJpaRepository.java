package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionCatalunyaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicitacionCatalunyaJpaRepository extends JpaRepository<LicitacionCatalunyaEntity, Long> {

    Page<LicitacionCatalunyaEntity> findByCodiExpedientContainingIgnoreCaseOrDenominacioContainingIgnoreCaseOrNomOrganContainingIgnoreCase(
            String codiExpedient,
            String denominacio,
            String nomOrgan,
            Pageable pageable
    );
}

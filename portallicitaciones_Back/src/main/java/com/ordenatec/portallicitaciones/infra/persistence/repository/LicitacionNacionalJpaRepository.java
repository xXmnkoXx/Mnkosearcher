package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionNacionalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicitacionNacionalJpaRepository extends JpaRepository<LicitacionNacionalEntity, Long> {

    @Query("""
        select l from LicitacionNacionalEntity l
        where (:q is null or :q = '' or
               lower(l.expediente) like lower(concat('%', :q, '%')) or
               lower(l.objeto) like lower(concat('%', :q, '%')) or
               lower(l.organoContratante) like lower(concat('%', :q, '%')))
          and l.estado in ('Publicada','PRE')
    """)
    Page<LicitacionNacionalEntity> searchEnPlazo(@Param("q") String q, Pageable pageable);

    @Query("""
        select l from LicitacionNacionalEntity l
        where (:q is null or :q = '' or
               lower(l.expediente) like lower(concat('%', :q, '%')) or
               lower(l.objeto) like lower(concat('%', :q, '%')) or
               lower(l.organoContratante) like lower(concat('%', :q, '%')))
          and (l.estado is null or l.estado not in ('Publicada','PRE'))
    """)
    Page<LicitacionNacionalEntity> searchVencidas(@Param("q") String q, Pageable pageable);

    @Query("""
        select l from LicitacionNacionalEntity l
        where (:q is null or :q = '' or
               lower(l.expediente) like lower(concat('%', :q, '%')) or
               lower(l.objeto) like lower(concat('%', :q, '%')) or
               lower(l.organoContratante) like lower(concat('%', :q, '%')))
    """)
    Page<LicitacionNacionalEntity> searchAll(@Param("q") String q, Pageable pageable);
}

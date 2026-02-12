package com.ordenatec.portallicitaciones.domain.port;

import com.ordenatec.portallicitaciones.domain.model.Licitacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.query.Param;

public interface LicitacionRepository {

    Licitacion save(Licitacion licitacion);

    Optional<Licitacion> findById(UUID id);
    Optional<Licitacion> findByUrlPublica(String urlPublica);
    Optional<Licitacion> findByExpediente(String expediente);
    Optional<UUID> findIdByExpediente(@Param("expediente") String expediente);

    List<Licitacion> findAll();
}

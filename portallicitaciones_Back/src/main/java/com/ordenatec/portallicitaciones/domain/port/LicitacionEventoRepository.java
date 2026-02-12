package com.ordenatec.portallicitaciones.domain.port;

import com.ordenatec.portallicitaciones.domain.model.LicitacionEvento;

import java.util.Optional;
import java.util.UUID;

public interface LicitacionEventoRepository {

    /**
     * Guarda un evento de licitación (un <entry> del ATOM).
     * Recomendación: en BD UNIQUE por atomEntryId para evitar duplicados.
     */
    void save(LicitacionEvento evento);

    Optional<LicitacionEvento> findById(UUID id);

    /**
     * Muy útil para deduplicación “limpia” antes de intentar save().
     * Si no lo implementas, puedes seguir usando try/catch con DataIntegrityViolationException.
     */
    Optional<LicitacionEvento> findByAtomEntryId(String atomEntryId);

    boolean existsByAtomEntryId(String atomEntryId);
}

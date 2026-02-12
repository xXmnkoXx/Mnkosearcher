package com.ordenatec.portallicitaciones.infra.persistence.adapter;

import com.ordenatec.portallicitaciones.domain.enums.EventoTipo;
import com.ordenatec.portallicitaciones.domain.model.LicitacionEvento;
import com.ordenatec.portallicitaciones.domain.port.LicitacionEventoRepository;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEventoEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionEventoJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaLicitacionEventoRepository implements LicitacionEventoRepository {

    private final LicitacionEventoJpaRepository eventoJpa;
    private final LicitacionJpaRepository licitacionJpa;

    public JpaLicitacionEventoRepository(
            LicitacionEventoJpaRepository eventoJpa,
            LicitacionJpaRepository licitacionJpa
    ) {
        this.eventoJpa = eventoJpa;
        this.licitacionJpa = licitacionJpa;
    }

    @Override
    @Transactional
    public void save(LicitacionEvento evento) {
        if (evento == null) return;

        String atomEntryId = safe(evento.getAtomEntryId());
        if (atomEntryId == null) return; // sin atomEntryId no podemos deduplicar

        // ✅ UPSERT por atom_entry_id
        Optional<LicitacionEventoEntity> existing = eventoJpa.findByAtomEntryId(atomEntryId);
        LicitacionEventoEntity e = existing.orElseGet(LicitacionEventoEntity::new);

        // FK obligatoria -> resolver licitación por expediente
        String expediente = safe(evento.getExpediente());
        if (expediente == null) return; // si no hay expediente, no podemos colgar el evento
        LicitacionEntity lic = licitacionJpa.findByExpediente(expediente)
                .orElseThrow(() -> new IllegalStateException("No existe licitación para expediente=" + expediente));

        e.setLicitacion(lic);

        // Identificadoresya lo solucion´ñ
        e.setAtomEntryId(atomEntryId);
        if (e.getUuid() == null) e.setUuid(UUID.randomUUID());

        // Atom timestamps
        e.setAtomUpdatedAt(evento.getAtomUpdatedAt());
        e.setAtomPublishedAt(evento.getAtomPublishedAt());

        EventoTipo tipo = evento.getTipo();
        if (tipo == null) tipo = EventoTipo.DESCONOCIDO;
        e.setTipo(tipo);

        // ✅ fecha (NOT NULL en BD) - si viene null, lo arregla @PrePersist
        if (e.getFecha() == null) {
            Instant f = (evento.getAtomUpdatedAt() != null) ? evento.getAtomUpdatedAt()
                    : (evento.getAtomPublishedAt() != null) ? evento.getAtomPublishedAt()
                    : Instant.now();
            e.setFecha(f);
        }

        // Otros campos (si tu domain los trae)
        e.setExpediente(expediente);
        e.setUrlPublica(evento.getUrlPublica());
        e.setSourceZip(evento.getSourceZip());
        e.setSourceAtomFile(evento.getSourceAtomFile());
        e.setRawEntryXml(evento.getRawEntryXml());
        e.setChangeSummary(evento.getChangeSummary());

        // ✅ si existía, esto hace UPDATE; si no existía, INSERT
        eventoJpa.save(e);
    }

    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public Optional<LicitacionEvento> findById(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public Optional<LicitacionEvento> findByAtomEntryId(String atomEntryId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByAtomEntryId'");
    }

    @Override
    public boolean existsByAtomEntryId(String atomEntryId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'existsByAtomEntryId'");
    }
}

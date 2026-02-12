package com.ordenatec.portallicitaciones.infra.persistence.entity;

import com.ordenatec.portallicitaciones.domain.enums.EventoTipo;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "licitacion_eventos",
        indexes = {
                @Index(name = "ix_eventos_licitacion_id", columnList = "licitacion_id"),
                @Index(name = "ix_eventos_atom_entry_id", columnList = "atom_entry_id", unique = true),
                @Index(name = "ix_eventos_expediente", columnList = "expediente")
        }
)
public class LicitacionEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK obligatoria: un evento pertenece a una licitación.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "licitacion_id", nullable = false)
    private LicitacionEntity licitacion;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "atom_entry_id", length = 300, nullable = false, unique = true)
    private String atomEntryId;

    @Column(name = "atom_updated_at")
    private Instant atomUpdatedAt;

    @Column(name = "atom_published_at")
    private Instant atomPublishedAt;

    /**
     * En BD es NOT NULL.
     */
    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "expediente", length = 120)
    private String expediente;

    @Column(name = "url_publica", columnDefinition = "NVARCHAR(2000)")
    private String urlPublica;

    @Column(name = "source_zip", columnDefinition = "NVARCHAR(500)")
    private String sourceZip;

    @Column(name = "source_atom_file", columnDefinition = "NVARCHAR(500)")
    private String sourceAtomFile;

    /**
     * ✅ ESTE ES EL XML BUENO (columna real que tienes poblada en BD)
     */
    @Lob
    @Column(name = "raw_entry_xml", columnDefinition = "NVARCHAR(MAX)")
    private String rawEntryXml;

    /**
     * ✅ En BD es NOT NULL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private EventoTipo tipo;

    @Column(name = "change_summary", columnDefinition = "NVARCHAR(2000)")
    private String changeSummary;

    public LicitacionEventoEntity() {}

    @PrePersist
    public void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();

        // ✅ Garantiza NOT NULL para 'fecha'
        if (fecha == null) {
            if (atomUpdatedAt != null) fecha = atomUpdatedAt;
            else if (atomPublishedAt != null) fecha = atomPublishedAt;
            else fecha = Instant.now();
        }

        // ✅ Garantiza NOT NULL para 'tipo'
        if (tipo == null) {
            tipo = EventoTipo.DESCONOCIDO;
        }
    }

    // ------- getters / setters -------

    public Long getId() { return id; }

    public LicitacionEntity getLicitacion() { return licitacion; }
    public void setLicitacion(LicitacionEntity licitacion) { this.licitacion = licitacion; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getAtomEntryId() { return atomEntryId; }
    public void setAtomEntryId(String atomEntryId) { this.atomEntryId = atomEntryId; }

    public Instant getAtomUpdatedAt() { return atomUpdatedAt; }
    public void setAtomUpdatedAt(Instant atomUpdatedAt) { this.atomUpdatedAt = atomUpdatedAt; }

    public Instant getAtomPublishedAt() { return atomPublishedAt; }
    public void setAtomPublishedAt(Instant atomPublishedAt) { this.atomPublishedAt = atomPublishedAt; }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }

    public String getExpediente() { return expediente; }
    public void setExpediente(String expediente) { this.expediente = expediente; }

    public String getUrlPublica() { return urlPublica; }
    public void setUrlPublica(String urlPublica) { this.urlPublica = urlPublica; }

    public String getSourceZip() { return sourceZip; }
    public void setSourceZip(String sourceZip) { this.sourceZip = sourceZip; }

    public String getSourceAtomFile() { return sourceAtomFile; }
    public void setSourceAtomFile(String sourceAtomFile) { this.sourceAtomFile = sourceAtomFile; }

    public String getRawEntryXml() { return rawEntryXml; }
    public void setRawEntryXml(String rawEntryXml) { this.rawEntryXml = rawEntryXml; }

    public EventoTipo getTipo() { return tipo; }
    public void setTipo(EventoTipo tipo) { this.tipo = tipo; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
}

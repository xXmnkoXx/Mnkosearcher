package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(
        name = "licitaciones",
        schema = "dbo",
        indexes = {
                @Index(name = "ix_licitaciones_uuid", columnList = "uuid", unique = true),
                // OJO: expediente puede ser null (PLCSP). El unique index en SQL Server permite múltiples NULL.
                @Index(name = "ix_licitaciones_expediente", columnList = "expediente", unique = true),
                @Index(name = "ix_licitaciones_url_publica", columnList = "url_publica"),
                @Index(name = "ix_licitaciones_fecha_publicacion", columnList = "fecha_publicacion")
        }
)
public class LicitacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    /**
     * ✅ PLCSP: algunas filas vienen sin expediente o con inconsistencias.
     * Por eso: nullable = true.
     *
     * Si quieres unicidad real (expediente o url si no hay expediente),
     * eso se hace con índices filtrados en SQL Server (no con JPA puro).
     */
    @Column(name = "expediente", length = 120)
    private String expediente;

    @Column(name = "titulo", columnDefinition = "NVARCHAR(2000)")
    private String titulo;

    @Column(name = "url_publica", columnDefinition = "NVARCHAR(2000)")
    private String urlPublica;

    @Column(name = "url_detalle", columnDefinition = "NVARCHAR(2000)")
    private String urlDetalle;

    @Column(name = "raw_atom_json", columnDefinition = "NVARCHAR(MAX)")
    private String rawAtomJson;

    @Column(name = "raw_detalle_xml", columnDefinition = "NVARCHAR(MAX)")
    private String rawDetalleXml;

    @Column(name = "fecha_ultima_actualizacion")
    private Instant fechaUltimaActualizacion;

    // ================== CAMPOS GENERALES ==================

    @Column(name = "entidad", columnDefinition = "NVARCHAR(500)")
    private String entidad;

    @Column(name = "tipo_contrato_texto", columnDefinition = "NVARCHAR(200)")
    private String tipoContratoTexto;

    @Column(name = "modalidad", columnDefinition = "NVARCHAR(200)")
    private String modalidad;

    @Column(name = "procedimiento", columnDefinition = "NVARCHAR(200)")
    private String procedimiento;

    @Column(name = "criterio_adjudicacion", columnDefinition = "NVARCHAR(500)")
    private String criterioAdjudicacion;

    @Column(name = "codigo_nuts", length = 20)
    private String codigoNuts;

    @Column(name = "lugar_ejecucion", columnDefinition = "NVARCHAR(500)")
    private String lugarEjecucion;

    @Column(name = "estado_texto", columnDefinition = "NVARCHAR(100)")
    private String estadoTexto;

    @Column(name = "motivo", columnDefinition = "NVARCHAR(2000)")
    private String motivo;

    @Column(name = "plazo_ejecucion", columnDefinition = "NVARCHAR(200)")
    private String plazoEjecucion;

    @Column(name = "fecha_publicacion")
    private LocalDate fechaPublicacion;

    @Column(name = "precio_licitacion", precision = 19, scale = 2)
    private BigDecimal precioLicitacion;

    @Column(name = "valor_estimado", precision = 19, scale = 2)
    private BigDecimal valorEstimado;

    @Column(name = "moneda", length = 3)
    private String moneda;

    @Column(name = "fecha_limite_presentacion")
    private LocalDate fechaLimitePresentacion;

    // ----------------- Relaciones -----------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organismo_id")
    private OrganismoEntity organismo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "licitacion_cpvs",
            schema = "dbo",
            joinColumns = @JoinColumn(name = "licitacion_id"),
            inverseJoinColumns = @JoinColumn(name = "cpv_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "UQ_licitacion_cpvs",
                    columnNames = {"licitacion_id", "cpv_id"}
            )
    )
    private Set<CpvEntity> cpvs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "licitacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoEntity> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "licitacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoteEntity> lotes = new ArrayList<>();

    /**
     * ✅ Adjudicación 1-1 (FK EN adjudicaciones.licitacion_id)
     */
    @OneToOne(mappedBy = "licitacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AdjudicacionEntity adjudicacion;

    public LicitacionEntity() {}

    // ----------------- Getters / Setters -----------------

    public Long getId() { return id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getExpediente() { return expediente; }
    public void setExpediente(String expediente) { this.expediente = expediente; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getUrlPublica() { return urlPublica; }
    public void setUrlPublica(String urlPublica) { this.urlPublica = urlPublica; }

    public String getUrlDetalle() { return urlDetalle; }
    public void setUrlDetalle(String urlDetalle) { this.urlDetalle = urlDetalle; }

    public String getRawAtomJson() { return rawAtomJson; }
    public void setRawAtomJson(String rawAtomJson) { this.rawAtomJson = rawAtomJson; }

    public String getRawDetalleXml() { return rawDetalleXml; }
    public void setRawDetalleXml(String rawDetalleXml) { this.rawDetalleXml = rawDetalleXml; }

    public Instant getFechaUltimaActualizacion() { return fechaUltimaActualizacion; }
    public void setFechaUltimaActualizacion(Instant fechaUltimaActualizacion) { this.fechaUltimaActualizacion = fechaUltimaActualizacion; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getTipoContratoTexto() { return tipoContratoTexto; }
    public void setTipoContratoTexto(String tipoContratoTexto) { this.tipoContratoTexto = tipoContratoTexto; }

    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }

    public String getProcedimiento() { return procedimiento; }
    public void setProcedimiento(String procedimiento) { this.procedimiento = procedimiento; }

    public String getCriterioAdjudicacion() { return criterioAdjudicacion; }
    public void setCriterioAdjudicacion(String criterioAdjudicacion) { this.criterioAdjudicacion = criterioAdjudicacion; }

    public String getCodigoNuts() { return codigoNuts; }
    public void setCodigoNuts(String codigoNuts) { this.codigoNuts = codigoNuts; }

    public String getLugarEjecucion() { return lugarEjecucion; }
    public void setLugarEjecucion(String lugarEjecucion) { this.lugarEjecucion = lugarEjecucion; }

    public String getEstadoTexto() { return estadoTexto; }
    public void setEstadoTexto(String estadoTexto) { this.estadoTexto = estadoTexto; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getPlazoEjecucion() { return plazoEjecucion; }
    public void setPlazoEjecucion(String plazoEjecucion) { this.plazoEjecucion = plazoEjecucion; }

    public LocalDate getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDate fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }

    public BigDecimal getPrecioLicitacion() { return precioLicitacion; }
    public void setPrecioLicitacion(BigDecimal precioLicitacion) { this.precioLicitacion = precioLicitacion; }

    public BigDecimal getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(BigDecimal valorEstimado) { this.valorEstimado = valorEstimado; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public LocalDate getFechaLimitePresentacion() { return fechaLimitePresentacion; }
    public void setFechaLimitePresentacion(LocalDate fechaLimitePresentacion) { this.fechaLimitePresentacion = fechaLimitePresentacion; }

    public OrganismoEntity getOrganismo() { return organismo; }
    public void setOrganismo(OrganismoEntity organismo) { this.organismo = organismo; }

    public Set<CpvEntity> getCpvs() { return cpvs; }
    public void setCpvs(Set<CpvEntity> cpvs) { this.cpvs = (cpvs != null) ? cpvs : new LinkedHashSet<>(); }

    public List<DocumentoEntity> getDocumentos() { return documentos; }
    public List<LoteEntity> getLotes() { return lotes; }

    public AdjudicacionEntity getAdjudicacion() { return adjudicacion; }

    public void setAdjudicacion(AdjudicacionEntity adjudicacion) {
        if (this.adjudicacion != null) {
            this.adjudicacion.setLicitacion(null);
        }
        this.adjudicacion = adjudicacion;
        if (adjudicacion != null) {
            adjudicacion.setLicitacion(this);
        }
    }

    // ----------------- Helpers -----------------

    public void addDocumento(DocumentoEntity doc) {
        if (doc == null) return;
        documentos.add(doc);
        doc.setLicitacion(this);
    }

    public void clearDocumentos() {
        for (DocumentoEntity d : documentos) d.setLicitacion(null);
        documentos.clear();
    }

    public void addLote(LoteEntity lote) {
        if (lote == null) return;
        lotes.add(lote);
        lote.setLicitacion(this);
    }

    public void clearLotes() {
        for (LoteEntity l : lotes) l.setLicitacion(null);
        lotes.clear();
    }

    // ----------------- equals/hashCode (uuid) -----------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LicitacionEntity that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}

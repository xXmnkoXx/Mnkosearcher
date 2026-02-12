package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "adjudicaciones",
        schema = "dbo",
        indexes = {
                @Index(name = "ix_adjudicaciones_uuid", columnList = "uuid", unique = true),
                @Index(name = "ix_adjudicaciones_adjudicatario_nif", columnList = "adjudicatario_nif"),
                @Index(name = "ix_adjudicaciones_licitacion", columnList = "licitacion_id", unique = true)
        }
)
public class AdjudicacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "licitacion_id", nullable = false, unique = true)
    private LicitacionEntity licitacion;

    @Column(name = "fecha_adjudicacion")
    private OffsetDateTime fechaAdjudicacion;

    @Column(name = "adjudicatario", length = 512)
    private String adjudicatario;

    @Column(name = "adjudicatario_nombre", length = 512)
    private String adjudicatarioNombre;

    @Column(name = "adjudicatario_nif", length = 24)
    private String adjudicatarioNif;

    @Column(name = "estado", length = 64)
    private String estado;

    @Column(name = "importe", precision = 18, scale = 2)
    private BigDecimal importe;

    @Column(name = "importe_adjudicado", precision = 18, scale = 2)
    private BigDecimal importeAdjudicado;

    @Column(name = "moneda", length = 8)
    private String moneda;

    @Column(name = "numero_ofertas")
    private Integer numeroOfertas;

    @Column(name = "criterio", length = 1000)
    private String criterio;

    public AdjudicacionEntity() {}

    public Long getId() { return id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public LicitacionEntity getLicitacion() { return licitacion; }
    public void setLicitacion(LicitacionEntity licitacion) { this.licitacion = licitacion; }

    public OffsetDateTime getFechaAdjudicacion() { return fechaAdjudicacion; }
    public void setFechaAdjudicacion(OffsetDateTime fechaAdjudicacion) { this.fechaAdjudicacion = fechaAdjudicacion; }

    public String getAdjudicatario() { return adjudicatario; }
    public void setAdjudicatario(String adjudicatario) { this.adjudicatario = adjudicatario; }

    public String getAdjudicatarioNombre() { return adjudicatarioNombre; }
    public void setAdjudicatarioNombre(String adjudicatarioNombre) { this.adjudicatarioNombre = adjudicatarioNombre; }

    public String getAdjudicatarioNif() { return adjudicatarioNif; }
    public void setAdjudicatarioNif(String adjudicatarioNif) { this.adjudicatarioNif = adjudicatarioNif; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public BigDecimal getImporteAdjudicado() { return importeAdjudicado; }
    public void setImporteAdjudicado(BigDecimal importeAdjudicado) { this.importeAdjudicado = importeAdjudicado; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public Integer getNumeroOfertas() { return numeroOfertas; }
    public void setNumeroOfertas(Integer numeroOfertas) { this.numeroOfertas = numeroOfertas; }

    public String getCriterio() { return criterio; }
    public void setCriterio(String criterio) { this.criterio = criterio; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdjudicacionEntity that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}

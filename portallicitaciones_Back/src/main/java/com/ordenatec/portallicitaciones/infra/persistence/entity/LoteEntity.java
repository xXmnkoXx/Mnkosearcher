package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "lotes",
        schema = "dbo",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_lotes_licitacion_numero",
                columnNames = {"licitacion_id", "numero"}
        ),
        indexes = {
                @Index(name = "ix_lotes_licitacion_id", columnList = "licitacion_id"),
                @Index(name = "ix_lotes_numero", columnList = "numero")
        }
)
public class LoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "licitacion_id", nullable = false)
    private LicitacionEntity licitacion;

    @Column(name = "numero")
    private Integer numero;

    @Column(name = "titulo", columnDefinition = "NVARCHAR(700)")
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "NVARCHAR(MAX)")
    private String descripcion;

    @Column(name = "importe", precision = 19, scale = 2)
    private BigDecimal importe;

    @Column(name = "moneda", length = 3)
    private String moneda;

    @Column(name = "incluye_iva")
    private Boolean incluyeIva;

    @Column(name = "iva", precision = 19, scale = 2)
    private BigDecimal iva;

    @Column(name = "cpv_principal", length = 20)
    private String cpvPrincipal;

    @Column(name = "criterios", columnDefinition = "NVARCHAR(MAX)")
    private String criterios;

    public LoteEntity() {}

    public Long getId() { return id; }

    public LicitacionEntity getLicitacion() { return licitacion; }
    public void setLicitacion(LicitacionEntity licitacion) { this.licitacion = licitacion; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public Boolean getIncluyeIva() { return incluyeIva; }
    public void setIncluyeIva(Boolean incluyeIva) { this.incluyeIva = incluyeIva; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public String getCpvPrincipal() { return cpvPrincipal; }
    public void setCpvPrincipal(String cpvPrincipal) { this.cpvPrincipal = cpvPrincipal; }

    public String getCriterios() { return criterios; }
    public void setCriterios(String criterios) { this.criterios = criterios; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoteEntity that)) return false;
        // Identidad natural en BD: licitación + número (cuando exista)
        return Objects.equals(licitacion != null ? licitacion.getId() : null, that.licitacion != null ? that.licitacion.getId() : null)
                && Objects.equals(numero, that.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licitacion != null ? licitacion.getId() : null, numero);
    }
}

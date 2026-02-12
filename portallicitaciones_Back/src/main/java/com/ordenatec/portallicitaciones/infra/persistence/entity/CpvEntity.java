package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "cpvs",
        schema = "dbo",
        indexes = {
                @Index(name = "ix_cpvs_codigo", columnList = "codigo", unique = true),
                @Index(name = "ix_cpvs_descripcion", columnList = "descripcion")
        }
)
public class CpvEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CPV: 8 dígitos (45100000) */
    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "descripcion", columnDefinition = "NVARCHAR(500)")
    private String descripcion;

    @ManyToMany(mappedBy = "cpvs", fetch = FetchType.LAZY)
    private Set<LicitacionEntity> licitaciones = new LinkedHashSet<>();

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Set<LicitacionEntity> getLicitaciones() { return licitaciones; }
    public void setLicitaciones(Set<LicitacionEntity> licitaciones) { this.licitaciones = licitaciones; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CpvEntity that)) return false;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}

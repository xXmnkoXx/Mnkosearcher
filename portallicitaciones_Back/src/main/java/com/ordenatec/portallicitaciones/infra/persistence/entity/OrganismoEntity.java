package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "organismos",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_organismos_codigo", columnNames = {"codigo"}),
                @UniqueConstraint(name = "UQ_organismos_uuid", columnNames = {"uuid"})
        },
        indexes = {
                @Index(name = "ix_organismos_dir3", columnList = "dir3"),
                @Index(name = "ix_organismos_nif", columnList = "nif")
        }
)
public class OrganismoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "codigo", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String codigo;

    @Column(name = "nombre", columnDefinition = "NVARCHAR(512)")
    private String nombre;

    @Column(name = "dir3", length = 32)
    private String dir3;

    @Column(name = "nif", length = 24)
    private String nif;

    public OrganismoEntity() {}

    public Long getId() { return id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDir3() { return dir3; }
    public void setDir3(String dir3) { this.dir3 = dir3; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganismoEntity that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}

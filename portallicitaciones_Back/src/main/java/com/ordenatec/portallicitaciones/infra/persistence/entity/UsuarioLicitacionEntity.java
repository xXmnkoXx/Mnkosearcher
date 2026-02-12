package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "usuario_licitaciones",
        schema = "dbo",
        uniqueConstraints = @UniqueConstraint(
                name = "UX_usuario_licitaciones_unique",
                columnNames = {"id_usuario", "tipo", "id_externo", "enlace"}
        )
)
public class UsuarioLicitacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_licitacion")
    private Integer idUsuarioLicitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo; // LICIT | MENOR

    @Column(name = "id_externo", length = 200)
    private String idExterno;

    @Column(name = "titulo", length = 800)
    private String titulo;

    @Column(name = "organismo", length = 500)
    private String organismo;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "importe", precision = 18, scale = 2)
    private BigDecimal importe;

    @Column(name = "enlace", length = 1200)
    private String enlace;

    @Column(name = "cpv", length = 100)
    private String cpv;

    @Column(name = "fuente", length = 80)
    private String fuente;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDate fechaEnvio;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public UsuarioLicitacionEntity() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // -------- Getters / Setters --------

    public Integer getIdUsuarioLicitacion() {
        return idUsuarioLicitacion;
    }

    public void setIdUsuarioLicitacion(Integer idUsuarioLicitacion) {
        this.idUsuarioLicitacion = idUsuarioLicitacion;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public void setIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getOrganismo() {
        return organismo;
    }

    public void setOrganismo(String organismo) {
        this.organismo = organismo;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public String getCpv() {
        return cpv;
    }

    public void setCpv(String cpv) {
        this.cpv = cpv;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDate fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}

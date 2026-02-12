package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", schema = "dbo")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    // ==========================
    // Datos "cliente" (ya existen)
    // ==========================

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "nombre_cliente", length = 200)
    private String nombreCliente;

    @Column(name = "email_destino", length = 255)
    private String emailDestino;

    @Column(name = "cpvs")
    private String cpvs;

    @Column(name = "importe_max", precision = 18, scale = 2)
    private BigDecimal importeMax;

    @Column(name = "tipo_contrato", length = 50)
    private String tipoContrato;

    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "desde_offset")
    private Integer desdeOffset;

    @Column(name = "hasta_offset")
    private Integer hastaOffset;

    @Column(name = "max_paginas")
    private Integer maxPaginas;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precio", precision = 18, scale = 2)
    private BigDecimal precio;

    // OJO: en tu tabla se llaman así
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_update")
    private LocalDateTime fechaUpdate;

    // ==========================
    // NUEVO: Empresa (FK) + campos admin
    // ==========================

    @Column(name = "id_empresa")
    private Integer idEmpresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", insertable = false, updatable = false)
    private EmpresaEntity empresa;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "suscripcion", length = 60)
    private String suscripcion;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "alertas_configuradas", nullable = false)
    private Integer alertasConfiguradas = 0;

    @Column(name = "recibidas", nullable = false)
    private Integer recibidas = 0;

    @Column(name = "favoritas", nullable = false)
    private Integer favoritas = 0;

    @Column(name = "ultimo_favorito")
    private LocalDateTime ultimoFavorito;

    // ==========================
    // GETTERS / SETTERS
    // ==========================

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getEmailDestino() {
        return emailDestino;
    }

    public void setEmailDestino(String emailDestino) {
        this.emailDestino = emailDestino;
    }

    public String getCpvs() {
        return cpvs;
    }

    public void setCpvs(String cpvs) {
        this.cpvs = cpvs;
    }

    public BigDecimal getImporteMax() {
        return importeMax;
    }

    public void setImporteMax(BigDecimal importeMax) {
        this.importeMax = importeMax;
    }

    public String getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(String tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getDesdeOffset() {
        return desdeOffset;
    }

    public void setDesdeOffset(Integer desdeOffset) {
        this.desdeOffset = desdeOffset;
    }

    public Integer getHastaOffset() {
        return hastaOffset;
    }

    public void setHastaOffset(Integer hastaOffset) {
        this.hastaOffset = hastaOffset;
    }

    public Integer getMaxPaginas() {
        return maxPaginas;
    }

    public void setMaxPaginas(Integer maxPaginas) {
        this.maxPaginas = maxPaginas;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUpdate() {
        return fechaUpdate;
    }

    public void setFechaUpdate(LocalDateTime fechaUpdate) {
        this.fechaUpdate = fechaUpdate;
    }

    public Integer getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Integer idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public EmpresaEntity getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getSuscripcion() {
        return suscripcion;
    }

    public void setSuscripcion(String suscripcion) {
        this.suscripcion = suscripcion;
    }

    public LocalDateTime getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDateTime fechaBaja) {
        this.fechaBaja = fechaBaja;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public Integer getAlertasConfiguradas() {
        return alertasConfiguradas;
    }

    public void setAlertasConfiguradas(Integer alertasConfiguradas) {
        this.alertasConfiguradas = alertasConfiguradas;
    }

    public Integer getRecibidas() {
        return recibidas;
    }

    public void setRecibidas(Integer recibidas) {
        this.recibidas = recibidas;
    }

    public Integer getFavoritas() {
        return favoritas;
    }

    public void setFavoritas(Integer favoritas) {
        this.favoritas = favoritas;
    }

    public LocalDateTime getUltimoFavorito() {
        return ultimoFavorito;
    }

    public void setUltimoFavorito(LocalDateTime ultimoFavorito) {
        this.ultimoFavorito = ultimoFavorito;
    }
}

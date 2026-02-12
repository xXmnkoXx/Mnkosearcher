package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "alertas")
public class AlertaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long idAlerta;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    // ====== NUEVO: Nombre visible de la alerta ======
    @Column(name = "nombre", length = 150)
    private String nombre;

    // ====== Parámetros (pestaña PARÁMETROS) ======
    @Column(name = "palabras_clave", length = 500)
    private String palabrasClave;

    @Column(name = "descripcion_actividad", length = 500)
    private String descripcionActividad;

    @Column(name = "tipos_contratos", length = 200)
    private String tiposContratos;

    @Column(name = "contratos_menores", nullable = false)
    private Boolean contratosMenores;

    @Column(name = "lugares", length = 200)
    private String lugares;

    @Column(name = "organos_contratacion", length = 300)
    private String organosContratacion;

    // ====== Campos que ya tenías ======
    @Column(name = "cpvs", nullable = false, length = 500)
    private String cpvs;

    // (Puedes dejarlo como “descripcion técnica” o usarlo como “resumen”.
    // Si a futuro prefieres, puedes depreciarlo y usar descripcionActividad.)
    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "importe_min", precision = 18, scale = 2)
    private BigDecimal importeMin;

    @Column(name = "importe_max", precision = 18, scale = 2)
    private BigDecimal importeMax;

    @Column(name = "activa", nullable = false)
    private Boolean activa;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // ====== Opciones de envío (pestaña ENVÍO) ======
    @Column(name = "frecuencia_envio", length = 20)
    private String frecuenciaEnvio; // DIARIAMENTE | SEMANALMENTE | INMEDIATO

    @Column(name = "hora_envio")
    private LocalTime horaEnvio; // 10:00

    @Column(name = "dia_semana", length = 10)
    private String diaSemana; // Lunes..Domingo

    @Column(name = "notificar_cambios", nullable = false)
    private Boolean notificarCambios;

    @Column(name = "email_1", length = 255)
    private String email1;

    @Column(name = "email_2", length = 255)
    private String email2;

    @Column(name = "email_3", length = 255)
    private String email3;

    // ---------- GETTERS Y SETTERS ----------

    public Long getIdAlerta() { return idAlerta; }
    public void setIdAlerta(Long idAlerta) { this.idAlerta = idAlerta; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPalabrasClave() { return palabrasClave; }
    public void setPalabrasClave(String palabrasClave) { this.palabrasClave = palabrasClave; }

    public String getDescripcionActividad() { return descripcionActividad; }
    public void setDescripcionActividad(String descripcionActividad) { this.descripcionActividad = descripcionActividad; }

    public String getTiposContratos() { return tiposContratos; }
    public void setTiposContratos(String tiposContratos) { this.tiposContratos = tiposContratos; }

    public Boolean getContratosMenores() { return contratosMenores; }
    public void setContratosMenores(Boolean contratosMenores) { this.contratosMenores = contratosMenores; }

    public String getLugares() { return lugares; }
    public void setLugares(String lugares) { this.lugares = lugares; }

    public String getOrganosContratacion() { return organosContratacion; }
    public void setOrganosContratacion(String organosContratacion) { this.organosContratacion = organosContratacion; }

    public String getCpvs() { return cpvs; }
    public void setCpvs(String cpvs) { this.cpvs = cpvs; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getImporteMin() { return importeMin; }
    public void setImporteMin(BigDecimal importeMin) { this.importeMin = importeMin; }

    public BigDecimal getImporteMax() { return importeMax; }
    public void setImporteMax(BigDecimal importeMax) { this.importeMax = importeMax; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFrecuenciaEnvio() { return frecuenciaEnvio; }
    public void setFrecuenciaEnvio(String frecuenciaEnvio) { this.frecuenciaEnvio = frecuenciaEnvio; }

    public LocalTime getHoraEnvio() { return horaEnvio; }
    public void setHoraEnvio(LocalTime horaEnvio) { this.horaEnvio = horaEnvio; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public Boolean getNotificarCambios() { return notificarCambios; }
    public void setNotificarCambios(Boolean notificarCambios) { this.notificarCambios = notificarCambios; }

    public String getEmail1() { return email1; }
    public void setEmail1(String email1) { this.email1 = email1; }

    public String getEmail2() { return email2; }
    public void setEmail2(String email2) { this.email2 = email2; }

    public String getEmail3() { return email3; }
    public void setEmail3(String email3) { this.email3 = email3; }
}

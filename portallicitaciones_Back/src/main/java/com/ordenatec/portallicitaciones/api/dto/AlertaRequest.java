package com.ordenatec.portallicitaciones.api.dto;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear/actualizar una alerta (UI completa).
 */
public class AlertaRequest {

    // Identidad (no viene en request)
    // private Long idAlerta;

    // === Campos base ===
    private String nombre;           // ✅ NUEVO: nombre visible
    private String cpvs;
    private String descripcion;      // puedes usarlo como "resumen" o dejarlo igual
    private String tipo;             // "SUMINISTRO" | "SERVICIOS"
    private BigDecimal importeMin;
    private BigDecimal importeMax;
    private Boolean activa;

    // === PARÁMETROS ===
    private String palabrasClave;
    private String descripcionActividad;
    private String tiposContratos;
    private Boolean contratosMenores;
    private String lugares;
    private String organosContratacion;

    // === ENVÍO ===
    private String frecuenciaEnvio;  // DIARIAMENTE | SEMANALMENTE | INMEDIATO
    private String horaEnvio;        // "10:00" (lo parseamos a LocalTime en service)
    private String diaSemana;        // Lunes..Domingo
    private Boolean notificarCambios;

    private String email1;
    private String email2;
    private String email3;

    public AlertaRequest() {}

    // -------- GETTERS/SETTERS --------

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

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

    public String getFrecuenciaEnvio() { return frecuenciaEnvio; }
    public void setFrecuenciaEnvio(String frecuenciaEnvio) { this.frecuenciaEnvio = frecuenciaEnvio; }

    public String getHoraEnvio() { return horaEnvio; }
    public void setHoraEnvio(String horaEnvio) { this.horaEnvio = horaEnvio; }

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

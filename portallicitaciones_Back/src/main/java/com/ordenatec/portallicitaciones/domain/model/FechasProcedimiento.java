package com.ordenatec.portallicitaciones.domain.model;

import java.time.*;

public class FechasProcedimiento {
    private LocalDate fechaPublicacion;
    private LocalDate fechaLimitePresentacion;
    private LocalDate fechaApertura;      // si viene
    private LocalDate fechaAdjudicacion;  // si viene
    private LocalDate fechaFormalizacion; // si viene
    public LocalDate getFechaPublicacion() {
        return fechaPublicacion;
    }
    public void setFechaPublicacion(LocalDate fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }
    public LocalDate getFechaLimitePresentacion() {
        return fechaLimitePresentacion;
    }
    public void setFechaLimitePresentacion(LocalDate fechaLimitePresentacion) {
        this.fechaLimitePresentacion = fechaLimitePresentacion;
    }
    public LocalDate getFechaApertura() {
        return fechaApertura;
    }
    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }
    public LocalDate getFechaAdjudicacion() {
        return fechaAdjudicacion;
    }
    public void setFechaAdjudicacion(LocalDate fechaAdjudicacion) {
        this.fechaAdjudicacion = fechaAdjudicacion;
    }
    public LocalDate getFechaFormalizacion() {
        return fechaFormalizacion;
    }
    public void setFechaFormalizacion(LocalDate fechaFormalizacion) {
        this.fechaFormalizacion = fechaFormalizacion;
    }
}

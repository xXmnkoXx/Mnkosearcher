package com.ordenatec.portallicitaciones.domain.model;

import java.util.*;

public class Lote {
    private String numero;   // a veces es "1", "Lote 1", etc.
    private String titulo;

    private Money presupuesto;
    private DuracionContrato duracion;

    private List<Cpv> cpvs = new ArrayList<>();

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Money getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(Money presupuesto) {
        this.presupuesto = presupuesto;
    }

    public DuracionContrato getDuracion() {
        return duracion;
    }

    public void setDuracion(DuracionContrato duracion) {
        this.duracion = duracion;
    }

    public List<Cpv> getCpvs() {
        return cpvs;
    }

    public void setCpvs(List<Cpv> cpvs) {
        this.cpvs = cpvs;
    }
}

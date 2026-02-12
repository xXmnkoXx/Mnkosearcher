package com.ordenatec.portallicitaciones.domain.model;

public class Adjudicacion {
    private String adjudicatarioNombre;
    private String adjudicatarioNif;

    private Money importeAdjudicado;
    private Integer numeroOfertas;

    private String criterio; // texto si viene

    public String getAdjudicatarioNombre() {
        return adjudicatarioNombre;
    }

    public void setAdjudicatarioNombre(String adjudicatarioNombre) {
        this.adjudicatarioNombre = adjudicatarioNombre;
    }

    public String getAdjudicatarioNif() {
        return adjudicatarioNif;
    }

    public void setAdjudicatarioNif(String adjudicatarioNif) {
        this.adjudicatarioNif = adjudicatarioNif;
    }

    public Money getImporteAdjudicado() {
        return importeAdjudicado;
    }

    public void setImporteAdjudicado(Money importeAdjudicado) {
        this.importeAdjudicado = importeAdjudicado;
    }

    public Integer getNumeroOfertas() {
        return numeroOfertas;
    }

    public void setNumeroOfertas(Integer numeroOfertas) {
        this.numeroOfertas = numeroOfertas;
    }

    public String getCriterio() {
        return criterio;
    }

    public void setCriterio(String criterio) {
        this.criterio = criterio;
    }
}

package com.ordenatec.portallicitaciones.domain.model;

public class DuracionContrato {
    private Integer meses;
    private Integer anios;
    private Boolean prorroga;
    private Integer numeroProrrogas;
    public Integer getMeses() {
        return meses;
    }
    public void setMeses(Integer meses) {
        this.meses = meses;
    }
    public Integer getAnios() {
        return anios;
    }
    public void setAnios(Integer anios) {
        this.anios = anios;
    }
    public Boolean getProrroga() {
        return prorroga;
    }
    public void setProrroga(Boolean prorroga) {
        this.prorroga = prorroga;
    }
    public Integer getNumeroProrrogas() {
        return numeroProrrogas;
    }
    public void setNumeroProrrogas(Integer numeroProrrogas) {
        this.numeroProrrogas = numeroProrrogas;
    }
}

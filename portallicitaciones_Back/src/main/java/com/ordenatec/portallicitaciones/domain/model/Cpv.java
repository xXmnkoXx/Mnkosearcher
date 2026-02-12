package com.ordenatec.portallicitaciones.domain.model;

public class Cpv {
    private String codigo;        // "30192000"
    private String descripcion;   // si la fuente lo trae o si tú la enriqueces luego
    private Boolean principal;
    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    public Boolean getPrincipal() {
        return principal;
    }
    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }
}

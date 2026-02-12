package com.ordenatec.portallicitaciones.domain.model;

public class OrganismoContratacion {
    private String nombre;
    private String dir3;
    private String nif;
    private String tipoAdministracion;

    private Direccion direccion;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDir3() {
        return dir3;
    }

    public void setDir3(String dir3) {
        this.dir3 = dir3;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getTipoAdministracion() {
        return tipoAdministracion;
    }

    public void setTipoAdministracion(String tipoAdministracion) {
        this.tipoAdministracion = tipoAdministracion;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }
}

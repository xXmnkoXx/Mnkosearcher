package com.ordenatec.portallicitaciones.domain.model;

public class Direccion {
    private String via;
    private String localidad;
    private String provincia;
    private String codigoPostal;
    private String pais;
    public String getVia() {
        return via;
    }
    public void setVia(String via) {
        this.via = via;
    }
    public String getLocalidad() {
        return localidad;
    }
    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }
    public String getProvincia() {
        return provincia;
    }
    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }
    public String getCodigoPostal() {
        return codigoPostal;
    }
    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }
    public String getPais() {
        return pais;
    }
    public void setPais(String pais) {
        this.pais = pais;
    }
}

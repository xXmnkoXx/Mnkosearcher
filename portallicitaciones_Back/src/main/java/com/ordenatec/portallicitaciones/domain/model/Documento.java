package com.ordenatec.portallicitaciones.domain.model;

import java.time.Instant;

public class Documento {
    private String tipo;      // pliego, anexo, aclaración...
    private String titulo;
    private String url;
    private Instant fecha;
    private String hash;      // opcional si más tarde descargas y calculas hash
    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public Instant getFecha() {
        return fecha;
    }
    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }
    public String getHash() {
        return hash;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }
}

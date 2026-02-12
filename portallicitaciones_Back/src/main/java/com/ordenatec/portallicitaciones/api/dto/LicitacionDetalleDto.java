package com.ordenatec.portallicitaciones.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class LicitacionDetalleDto {

    public UUID id;
    public String expediente;
    public String referenciaPlataforma;
    public String titulo;
    public String urlPublica;

    public String organismo;
    public String estado;

    public LocalDate fechaPublicacion;
    public LocalDate fechaLimite;

    public BigDecimal presupuestoBase;
    public BigDecimal valorEstimado;
    public String moneda;

    public String lugarEjecucion;
    public String entidad;
    public String organo;

    public List<String> cpvs;
    public List<DocumentoDto> documentos;

    public LicitacionDetalleDto(
            UUID id,
            String expediente,
            String referenciaPlataforma,
            String titulo,
            String urlPublica,
            String organismo,
            String estado,
            LocalDate fechaPublicacion,
            LocalDate fechaLimite,
            BigDecimal presupuestoBase,
            BigDecimal valorEstimado,
            String moneda,
            String lugarEjecucion,
            String entidad,
            String organo,
            List<String> cpvs,
            List<DocumentoDto> documentos
    ) {
        this.id = id;
        this.expediente = expediente;
        this.referenciaPlataforma = referenciaPlataforma;
        this.titulo = titulo;
        this.urlPublica = urlPublica;
        this.organismo = organismo;
        this.estado = estado;
        this.fechaPublicacion = fechaPublicacion;
        this.fechaLimite = fechaLimite;
        this.presupuestoBase = presupuestoBase;
        this.valorEstimado = valorEstimado;
        this.moneda = moneda;
        this.lugarEjecucion = lugarEjecucion;
        this.entidad = entidad;
        this.organo = organo;
        this.cpvs = cpvs;
        this.documentos = documentos;
    }

    public static class DocumentoDto {
        public String tipo;
        public String nombre;
        public String url;

        public DocumentoDto(String tipo, String nombre, String url) {
            this.tipo = tipo;
            this.nombre = nombre;
            this.url = url;
        }
    }
}

package com.ordenatec.portallicitaciones.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO completo para detalle de licitación.
 * Devuelve TODO lo que tengas en BD y el front decide qué mostrar.
 */
public class LicitacionFullDto {

    // --- Identidad / tracking ---
    public Long idDb;
    public UUID uuid;
    public Instant fechaUltimaActualizacion;

    // --- Core ---
    public String expediente;
    public String titulo;

    // --- URLs / raw (muy útil para debug) ---
    public String urlPublica;
    public String urlDetalle;
    public String rawAtomJson;
    public String rawDetalleXml;

    // --- Campos texto “generales” ---
    public String entidad;
    public String organo;
    public String procedimiento;
    public String tipoContratoTexto;
    public String modalidad;
    public String criterioAdjudicacion;
    public String codigoNuts;
    public String lugarEjecucion;
    public String estadoTexto;
    public String motivo;
    public String plazoEjecucion;

    // --- Fechas / importes ---
    public LocalDate fechaPublicacion;
    public LocalDate fechaLimitePresentacion;
    public BigDecimal precioLicitacion;
    public BigDecimal valorEstimado;
    public String moneda;

    // --- Relaciones / bloques ---
    public OrganismoDto organismo;
    public List<String> cpvs;
    public List<DocumentoDto> documentos;
    public List<LoteDto> lotes;
    public AdjudicacionDto adjudicacion;

    // ===========================
    // Sub-DTOs
    // ===========================

    public static class OrganismoDto {
        public Long id;
        public String nombre;
        public String nif;
        public String codigo;

        public OrganismoDto() {}

        public OrganismoDto(Long id, String nombre, String nif, String codigo) {
            this.id = id;
            this.nombre = nombre;
            this.nif = nif;
            this.codigo = codigo;
        }
    }

    public static class DocumentoDto {
        public Long id;
        public String tipo;
        public String titulo;
        public String url;

        public DocumentoDto() {}

        public DocumentoDto(Long id, String tipo, String titulo, String url) {
            this.id = id;
            this.tipo = tipo;
            this.titulo = titulo;
            this.url = url;
        }
    }

    public static class LoteDto {
        public Long id;
        public String titulo;
        public BigDecimal importe;
        public String moneda;

        public LoteDto() {}

        public LoteDto(Long id, String titulo, BigDecimal importe, String moneda) {
            this.id = id;
            this.titulo = titulo;
            this.importe = importe;
            this.moneda = moneda;
        }
    }

    public static class AdjudicacionDto {
        public Long id;
        public String adjudicatario;
        public BigDecimal importe;
        public String moneda;
        public LocalDate fecha;

        public AdjudicacionDto() {}

        public AdjudicacionDto(Long id, String adjudicatario, BigDecimal importe, String moneda, LocalDate fecha) {
            this.id = id;
            this.adjudicatario = adjudicatario;
            this.importe = importe;
            this.moneda = moneda;
            this.fecha = fecha;
        }
    }
}

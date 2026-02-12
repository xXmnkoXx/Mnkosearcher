package com.ordenatec.portallicitaciones.infra.detalle;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resultado del parseo del HTML/Detalle.
 * Incluye tanto campos funcionales de la licitación
 * como metadatos técnicos del fetch.
 */
public class DetalleExtraido {

    // --------- Metadatos técnicos ---------
    private String urlFinal;
    private String contentType;

    // --------- Datos de licitación ---------
    private String entidad;
    private String organo;
    private String estadoTexto;
    private String criterioAdjudicacion;
    private String codigoNuts;
    private String lugarEjecucion;
    private String motivo;
    private String plazoEjecucion;
    private String modalidad;
    private String procedimiento;

    // --------- Extensiones (CPVs / importes) ---------
    private Set<String> cpvCodes = new LinkedHashSet<>();
    private BigDecimal presupuesto;
    private BigDecimal valorEstimado;
    private String moneda;

    /* ================= FACTORIES ================= */

    public static DetalleExtraido vacio() {
        // deja estructuras inicializadas
        DetalleExtraido d = new DetalleExtraido();
        d.cpvCodes = new LinkedHashSet<>();
        return d;
    }

    public static Builder builder() {
        return new Builder();
    }

    /* ================= GETTERS ================= */

    public String getUrlFinal() { return urlFinal; }
    public String getContentType() { return contentType; }

    public String getEntidad() { return entidad; }
    public String getOrgano() { return organo; }
    public String getEstadoTexto() { return estadoTexto; }
    public String getCriterioAdjudicacion() { return criterioAdjudicacion; }
    public String getCodigoNuts() { return codigoNuts; }
    public String getLugarEjecucion() { return lugarEjecucion; }
    public String getMotivo() { return motivo; }
    public String getPlazoEjecucion() { return plazoEjecucion; }
    public String getModalidad() { return modalidad; }
    public String getProcedimiento() { return procedimiento; }

    public Set<String> getCpvCodes() { return cpvCodes; }
    public BigDecimal getPresupuesto() { return presupuesto; }
    public BigDecimal getValorEstimado() { return valorEstimado; }
    public String getMoneda() { return moneda; }

    /* ================= SETTERS (fallback / técnicos) ================= */

    public void setUrlFinal(String urlFinal) {
        this.urlFinal = urlFinal;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /* ================= BUILDER ================= */

    public static class Builder {
        private final DetalleExtraido d = new DetalleExtraido();

        // técnicos
        public Builder urlFinal(String v) { d.urlFinal = v; return this; }
        public Builder contentType(String v) { d.contentType = v; return this; }

        // funcionales
        public Builder entidad(String v) { d.entidad = v; return this; }
        public Builder organo(String v) { d.organo = v; return this; }
        public Builder estadoTexto(String v) { d.estadoTexto = v; return this; }
        public Builder criterioAdjudicacion(String v) { d.criterioAdjudicacion = v; return this; }
        public Builder codigoNuts(String v) { d.codigoNuts = v; return this; }
        public Builder lugarEjecucion(String v) { d.lugarEjecucion = v; return this; }
        public Builder motivo(String v) { d.motivo = v; return this; }
        public Builder plazoEjecucion(String v) { d.plazoEjecucion = v; return this; }
        public Builder modalidad(String v) { d.modalidad = v; return this; }
        public Builder procedimiento(String v) { d.procedimiento = v; return this; }

        // extensiones
        public Builder cpvCodes(Set<String> v) {
            if (v != null) d.cpvCodes = new LinkedHashSet<>(v);
            return this;
        }
        public Builder presupuesto(BigDecimal v) { d.presupuesto = v; return this; }
        public Builder valorEstimado(BigDecimal v) { d.valorEstimado = v; return this; }
        public Builder moneda(String v) { d.moneda = v; return this; }

        public DetalleExtraido build() {
            // defensivo: nunca devolver null en set
            if (d.cpvCodes == null) d.cpvCodes = new LinkedHashSet<>();
            return d;
        }
    }
}

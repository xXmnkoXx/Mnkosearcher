package com.ordenatec.portallicitaciones.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ordenatec.portallicitaciones.domain.enums.EstadoLicitacion;
import com.ordenatec.portallicitaciones.domain.enums.TipoContrato;
import com.ordenatec.portallicitaciones.infra.persistence.entity.OrganismoEntity;

public class Licitacion {

    private UUID id;                    // interno
    private String expediente;           // clave humana (si existe)
    private String referenciaPlataforma; // si viene en el XML
    private String titulo;
    private String urlPublica;

    private OrganismoContratacion organismo;

    private TipoContrato tipoContrato;
    private String procedimiento;
    private String tramitacion;
    private Boolean sra;                 // sujeto a regulación armonizada

    // ===== Navarra / fuentes autonómicas (campos “texto” adicionales) =====
    private String entidad;
    private String organo;
    private String criterioAdjudicacion;
    private String codigoNuts;
    private String lugarEjecucion;
    private String motivo;
    private String plazoEjecucion;

    private EstadoLicitacion estado;     // “foto actual”
    private FechasProcedimiento fechas;

    private Money presupuestoBase;
    private Money valorEstimado;
    private String moneda;

    private List<Cpv> cpvs = new ArrayList<>();
    private List<Lote> lotes = new ArrayList<>();
    private List<Documento> documentos = new ArrayList<>();

    private Adjudicacion adjudicacion;

    private String lastEventEntryId;
    private Instant lastEventUpdatedAt;

    // =======================
    // GETTERS Y SETTERS
    // =======================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExpediente() {
        return expediente;
    }

    public void setExpediente(String expediente) {
        this.expediente = expediente;
    }

    public String getReferenciaPlataforma() {
        return referenciaPlataforma;
    }

    public void setReferenciaPlataforma(String referenciaPlataforma) {
        this.referenciaPlataforma = referenciaPlataforma;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getUrlPublica() {
        return urlPublica;
    }

    public void setUrlPublica(String urlPublica) {
        this.urlPublica = urlPublica;
    }

    public OrganismoContratacion getOrganismo() {
        return organismo;
    }

    public void setOrganismo(OrganismoContratacion organismo) {
        this.organismo = organismo;
    }

    public TipoContrato getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(TipoContrato tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public String getProcedimiento() {
        return procedimiento;
    }

    public void setProcedimiento(String procedimiento) {
        this.procedimiento = procedimiento;
    }

    public String getTramitacion() {
        return tramitacion;
    }

    public void setTramitacion(String tramitacion) {
        this.tramitacion = tramitacion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getOrgano() {
        return organo;
    }

    public String getCriterioAdjudicacion() {
        return criterioAdjudicacion;
    }

    public void setCriterioAdjudicacion(String criterioAdjudicacion) {
        this.criterioAdjudicacion = criterioAdjudicacion;
    }

    public String getCodigoNuts() {
        return codigoNuts;
    }

    public void setCodigoNuts(String codigoNuts) {
        this.codigoNuts = codigoNuts;
    }

    public String getLugarEjecucion() {
        return lugarEjecucion;
    }

    public void setLugarEjecucion(String lugarEjecucion) {
        this.lugarEjecucion = lugarEjecucion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getPlazoEjecucion() {
        return plazoEjecucion;
    }

    public void setPlazoEjecucion(String plazoEjecucion) {
        this.plazoEjecucion = plazoEjecucion;
    }

    public Boolean getSra() {
        return sra;
    }

    public void setSra(Boolean sra) {
        this.sra = sra;
    }

    public EstadoLicitacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoLicitacion estado) {
        this.estado = estado;
    }

    public FechasProcedimiento getFechas() {
        return fechas;
    }

    public void setFechas(FechasProcedimiento fechas) {
        this.fechas = fechas;
    }

    public Money getPresupuestoBase() {
        return presupuestoBase;
    }

    public void setPresupuestoBase(Money presupuestoBase) {
        this.presupuestoBase = presupuestoBase;
    }

    public Money getValorEstimado() {
        return valorEstimado;
    }

    public void setValorEstimado(Money valorEstimado) {
        this.valorEstimado = valorEstimado;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public List<Cpv> getCpvs() {
        return cpvs;
    }

    public void setCpvs(List<Cpv> cpvs) {
        this.cpvs = cpvs;
    }

    public List<Lote> getLotes() {
        return lotes;
    }

    public void setLotes(List<Lote> lotes) {
        this.lotes = lotes;
    }

    public List<Documento> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<Documento> documentos) {
        this.documentos = documentos;
    }

    public Adjudicacion getAdjudicacion() {
        return adjudicacion;
    }

    public void setAdjudicacion(Adjudicacion adjudicacion) {
        this.adjudicacion = adjudicacion;
    }

    public String getLastEventEntryId() {
        return lastEventEntryId;
    }

    public void setLastEventEntryId(String lastEventEntryId) {
        this.lastEventEntryId = lastEventEntryId;
    }

    public Instant getLastEventUpdatedAt() {
        return lastEventUpdatedAt;
    }

    public void setLastEventUpdatedAt(Instant lastEventUpdatedAt) {
        this.lastEventUpdatedAt = lastEventUpdatedAt;
    }
}

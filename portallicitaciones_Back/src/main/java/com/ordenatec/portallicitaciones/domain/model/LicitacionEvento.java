package com.ordenatec.portallicitaciones.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.ordenatec.portallicitaciones.domain.enums.EventoTipo;

public class LicitacionEvento {
    private UUID id;                  // interno
    private String atomEntryId;       // <id> del entry (CLAVE ÚNICA para no duplicar)
    private Instant atomUpdatedAt;    // <updated>
    private Instant atomPublishedAt;  // <published> si existe

    private String sourceZip;         // nombre del zip o url
    private String sourceAtomFile;    // nombre del .atom dentro del zip

    // vínculo: cómo lo asociamos a la Licitacion
    private String expediente;        // si viene
    private String urlPublica;        // link alternate
    private UUID licitacionId;        // si ya existe internamente

    private EventoTipo tipo;          // PUBLICACION / CORRECCION / ADJUDICACION / ANULACION / etc.

    // payload crudo (para “guardar TODO” de verdad)
    private String rawContentXml;     // <content> completo (string)
    private String rawEntryXml;       // opcional: entry completo

    // resumen de cambios (opcional pero útil)
    private String changeSummary;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAtomEntryId() {
        return atomEntryId;
    }

    public void setAtomEntryId(String atomEntryId) {
        this.atomEntryId = atomEntryId;
    }

    public Instant getAtomUpdatedAt() {
        return atomUpdatedAt;
    }

    public void setAtomUpdatedAt(Instant atomUpdatedAt) {
        this.atomUpdatedAt = atomUpdatedAt;
    }

    public Instant getAtomPublishedAt() {
        return atomPublishedAt;
    }

    public void setAtomPublishedAt(Instant atomPublishedAt) {
        this.atomPublishedAt = atomPublishedAt;
    }

    public String getSourceZip() {
        return sourceZip;
    }

    public void setSourceZip(String sourceZip) {
        this.sourceZip = sourceZip;
    }

    public String getSourceAtomFile() {
        return sourceAtomFile;
    }

    public void setSourceAtomFile(String sourceAtomFile) {
        this.sourceAtomFile = sourceAtomFile;
    }

    public String getExpediente() {
        return expediente;
    }

    public void setExpediente(String expediente) {
        this.expediente = expediente;
    }

    public String getUrlPublica() {
        return urlPublica;
    }

    public void setUrlPublica(String urlPublica) {
        this.urlPublica = urlPublica;
    }

    public UUID getLicitacionId() {
        return licitacionId;
    }

    public void setLicitacionId(UUID licitacionId) {
        this.licitacionId = licitacionId;
    }

    public EventoTipo getTipo() {
        return tipo;
    }

    public void setTipo(EventoTipo tipo) {
        this.tipo = tipo;
    }

    public String getRawContentXml() {
        return rawContentXml;
    }

    public void setRawContentXml(String rawContentXml) {
        this.rawContentXml = rawContentXml;
    }

    public String getRawEntryXml() {
        return rawEntryXml;
    }

    public void setRawEntryXml(String rawEntryXml) {
        this.rawEntryXml = rawEntryXml;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    // getters/setters
}

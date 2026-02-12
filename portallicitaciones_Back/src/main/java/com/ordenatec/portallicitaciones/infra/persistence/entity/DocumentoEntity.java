package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "documentos",
        schema = "dbo",
        indexes = {
                @Index(name = "ix_documentos_licitacion_id", columnList = "licitacion_id"),
                @Index(name = "ix_documentos_url", columnList = "url")
        }
)
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "licitacion_id", nullable = false)
    private LicitacionEntity licitacion;

    @Column(name = "tipo", length = 120)
    private String tipo;

    @Column(name = "titulo", columnDefinition = "NVARCHAR(500)")
    private String titulo;

    @Column(name = "url", columnDefinition = "NVARCHAR(2000)")
    private String url;

    @Column(name = "formato", length = 50)
    private String formato;

    @Column(name = "hash", length = 128)
    private String hash;

    @Column(name = "idioma", length = 10)
    private String idioma;

    @Column(name = "es_principal")
    private Boolean esPrincipal;

    public DocumentoEntity() {}

    public Long getId() { return id; }

    public LicitacionEntity getLicitacion() { return licitacion; }
    public void setLicitacion(LicitacionEntity licitacion) { this.licitacion = licitacion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public Boolean getEsPrincipal() { return esPrincipal; }
    public void setEsPrincipal(Boolean esPrincipal) { this.esPrincipal = esPrincipal; }
}

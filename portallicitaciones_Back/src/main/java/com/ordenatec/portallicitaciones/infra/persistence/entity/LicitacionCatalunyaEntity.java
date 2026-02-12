package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "licitacionesCatalunya", schema = "dbo",
        indexes = {
                @Index(name = "ix_licitacionesCatalunya_expedient", columnList = "codi_expedient"),
                @Index(name = "ix_licitacionesCatalunya_publicacio", columnList = "data_publicacio_anunci")
        })
public class LicitacionCatalunyaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LicitacionCatalunyaId")
    private Long licitacionCatalunyaId;

    @Column(name = "enllac_publicacio", length = 2000)
    private String enllacPublicacio;

    @Column(name = "codi_expedient", length = 200)
    private String codiExpedient;

    @Column(name = "denominacio", length = 2000)
    private String denominacio;

    @Column(name = "objecte_contracte", length = 4000)
    private String objecteContracte;

    @Column(name = "nom_organ", length = 1000)
    private String nomOrgan;

    @Column(name = "codi_dir3", length = 50)
    private String codiDir3;

    @Column(name = "nom_departament_ens", length = 1000)
    private String nomDepartamentEns;

    @Column(name = "tipus_contracte", length = 200)
    private String tipusContracte;

    @Column(name = "procediment", length = 200)
    private String procediment;

    @Column(name = "fase_publicacio", length = 200)
    private String fasePublicacio;

    @Column(name = "codi_cpv", length = 4000)
    private String codiCpv;

    @Column(name = "codi_nuts", length = 100)
    private String codiNuts;

    @Column(name = "lloc_execucio", length = 1000)
    private String llocExecucio;

    @Column(name = "valor_estimat_contracte", precision = 18, scale = 2)
    private BigDecimal valorEstimatContracte;

    @Column(name = "pressupost_licitacio_sense_iva", precision = 18, scale = 2)
    private BigDecimal pressupostLicitacioSenseIva;

    @Column(name = "pressupost_licitacio_amb_iva", precision = 18, scale = 2)
    private BigDecimal pressupostLicitacioAmbIva;

    @Column(name = "import_adjudicacio_sense_iva", precision = 18, scale = 2)
    private BigDecimal importAdjudicacioSenseIva;

    @Column(name = "import_adjudicacio_amb_iva", precision = 18, scale = 2)
    private BigDecimal importAdjudicacioAmbIva;

    @Column(name = "denominacio_adjudicatari", length = 2000)
    private String denominacioAdjudicatari;

    @Column(name = "identificacio_adjudicatari", length = 100)
    private String identificacioAdjudicatari;

    @Column(name = "ofertes_rebudes")
    private Integer ofertesRebudes;

    @Column(name = "data_publicacio_anunci")
    private LocalDateTime dataPublicacioAnunci;

    @Column(name = "termini_presentacio_ofertes")
    private LocalDateTime terminiPresentacioOfertes;

    @Column(name = "data_adjudicacio_contracte")
    private LocalDateTime dataAdjudicacioContracte;

    @Column(name = "durada_contracte", length = 200)
    private String duradaContracte;



    
}

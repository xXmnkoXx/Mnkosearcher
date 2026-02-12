package com.ordenatec.portallicitaciones.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "licitacionesNacional",
        schema = "dbo",
        indexes = {
                @Index(name = "ix_licitacionesNacional_id_fecha_updated", columnList = "id, fecha_updated"),
                @Index(name = "ix_licitacionesNacional_expediente", columnList = "expediente"),
                @Index(name = "ix_licitacionesNacional_fecha_publicacion", columnList = "fecha_publicacion")
        }
)
public class LicitacionNacionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LicitacionNacionalId")
    private Long licitacionNacionalId;

    @Column(name = "id", nullable = false, length = 256)
    private String id;

    @Column(name = "expediente", length = 50)
    private String expediente;

    @Column(name = "objeto", nullable = false, length = 2000)
    private String objeto;

    @Column(name = "organo_contratante", nullable = false, length = 300)
    private String organoContratante;

    @Column(name = "nif_organo", length = 15)
    private String nifOrgano;

    @Column(name = "dir3_organo", length = 15)
    private String dir3Organo;

    @Column(name = "id_plataforma", length = 32)
    private String idPlataforma;

    @Column(name = "ciudad_organo", length = 50)
    private String ciudadOrgano;

    @Column(name = "dependencia", length = 800)
    private String dependencia;

    @Column(name = "tipo_contrato_code", length = 10)
    private String tipoContratoCode;

    @Column(name = "tipo_contrato", length = 50)
    private String tipoContrato;

    @Column(name = "subtipo_code", length = 10)
    private String subtipoCode;

    @Column(name = "procedimiento_code", length = 10)
    private String procedimientoCode;

    @Column(name = "procedimiento", nullable = false, length = 50)
    private String procedimiento;

    @Column(name = "estado_code", nullable = false, length = 10)
    private String estadoCode;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "importe_sin_iva", precision = 18, scale = 2)
    private BigDecimal importeSinIva;

    @Column(name = "importe_con_iva", precision = 18, scale = 2)
    private BigDecimal importeConIva;

    @Column(name = "importe_adjudicacion", precision = 18, scale = 2)
    private BigDecimal importeAdjudicacion;

    @Column(name = "importe_adj_con_iva", precision = 18, scale = 2)
    private BigDecimal importeAdjConIva;

    @Column(name = "adjudicatario", length = 200)
    private String adjudicatario;

    @Column(name = "nif_adjudicatario", length = 40)
    private String nifAdjudicatario;

    @Column(name = "num_ofertas")
    private Integer numOfertas;

    @Column(name = "es_pyme")
    private Boolean esPyme;

    @Column(name = "cpv_principal", length = 16)
    private String cpvPrincipal;

    @Column(name = "cpvs", length = 4000)
    private String cpvs;

    @Column(name = "ubicacion", length = 50)
    private String ubicacion;

    @Column(name = "nuts", length = 10)
    private String nuts;

    @Column(name = "duracion", precision = 10, scale = 1)
    private BigDecimal duracion;

    @Column(name = "duracion_unidad", length = 10)
    private String duracionUnidad;

    @Column(name = "financiacion_ue", length = 10)
    private String financiacionUe;

    @Column(name = "urgencia", precision = 3, scale = 1)
    private BigDecimal urgencia;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Column(name = "hora_limite")
    private LocalTime horaLimite;

    @Column(name = "fecha_adjudicacion")
    private LocalDateTime fechaAdjudicacion;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_updated")
    private LocalDateTime fechaUpdated;

    @Column(name = "url", nullable = false, length = 256)
    private String url;

    @Column(name = "conjunto", nullable = false, length = 20)
    private String conjunto;

    @Column(name = "archivo_origen", nullable = false, length = 100)
    private String archivoOrigen;

    @Column(name = "ano", nullable = false)
    private Short ano;

    @Column(name = "tipo_registro", nullable = false, length = 10)
    private String tipoRegistro;

    @Column(name = "id_consulta", length = 50)
    private String idConsulta;

    @Column(name = "nombre_consulta", length = 800)
    private String nombreConsulta;

    @Column(name = "condiciones", length = 1200)
    private String condiciones;

    @Column(name = "tipo_condicion", length = 10)
    private String tipoCondicion;

    @Column(name = "fecha_planificada")
    private LocalDateTime fechaPlanificada;

    @Column(name = "fecha_limite_respuestas")
    private LocalDateTime fechaLimiteRespuestas;
}

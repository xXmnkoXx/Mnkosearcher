package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapeo de cabeceras del XLSX (OpenPLACSP) a setters de tu LicitacionEntity.
 *
 * - Clave: nombre EXACTO de la columna en el XLSX
 * - Valor: nombre del setter (ej: "setOrgano")
 *
 * Si un setter no existe en tu entity -> EntitySetter lo ignora sin romper.
 *
 * Ajusta/añade lo que quieras según tu tabla actual.
 */
public final class PlcspFieldMapping {

    private PlcspFieldMapping() {}

    /**
     * Campos “cabecera” de la licitación (no de lote/adjudicación).
     */
    public static Map<String, String> licitacionHeaderToSetter() {
        Map<String, String> m = new LinkedHashMap<>();

        // Identificación / links
        m.put("Identificador", "setPlacspIdentificador");
        m.put("Link licitación", "setUrlPublica");
        m.put("Fecha actualización", "setFechaActualizacionTexto"); // o setFechaActualizacion
        m.put("Primera publicación", "setFechaPublicacion");        // si tu entity usa LocalDate
        m.put("Vigente/Anulada/Archivada", "setEstadoVigencia");    // texto

        // Estado / expediente / título
        m.put("Estado", "setEstadoTexto");
        m.put("Número de expediente", "setExpediente");
        m.put("Objeto del Contrato", "setTitulo");
        m.put("Identificador único TED", "setTedId");

        // Importes
        m.put("Valor estimado del contrato", "setValorEstimado");
        m.put("Presupuesto base sin impuestos", "setPresupuestoSinImpuestos");
        m.put("Presupuesto base con impuestos", "setPrecioLicitacion"); // o setPresupuestoConImpuestos

        // CPV / tipo contrato
        m.put("CPV", "setCpv");
        m.put("Tipo de contrato", "setTipoContratoTexto");
        m.put("Contrato mixto", "setContratoMixto");

        // Lugar / OC
        m.put("Lugar de ejecución", "setLugarEjecucion");
        m.put("Órgano de Contratación", "setOrgano");
        m.put("ID OC en PLACSP", "setOrganoPlacspId");
        m.put("NIF OC", "setNifOrgano");
        m.put("DIR3", "setDir3");
        m.put("Enlace al Perfil de Contratante del OC", "setUrlPerfilContratante");
        m.put("Tipo de Administración", "setTipoAdministracion");
        m.put("Código Postal", "setCodigoPostal");

        // Procedimiento
        m.put("Tipo de procedimiento", "setProcedimiento");
        m.put("Sistema de contratación", "setSistemaContratacion");
        m.put("Tramitación", "setTramitacion");
        m.put("Forma de presentación de la oferta", "setFormaPresentacionOferta");

        // Fechas de presentación
        m.put("Fecha de presentación de ofertas", "setFechaLimitePresentacion");
        m.put("Fecha de presentación de solicitudes de participacion", "setFechaLimiteSolicitudes");

        // Directivas / SARA / UE
        m.put("Directiva de aplicación", "setDirectivaAplicacion");
        m.put("Contrato SARA/Umbral", "setSaraUmbral");
        m.put("Financiación Europea y fuente", "setFinanciacionEuropeaFuente");
        m.put("Descripción de la financiación europea", "setFinanciacionEuropeaDescripcion");

        // Otros
        m.put("Subasta electrónica", "setSubastaElectronica");
        m.put("Subcontratación permitida", "setSubcontratacionPermitida");
        m.put("Subcontratación permitida porcentaje", "setSubcontratacionPermitidaPorcentaje");

        return m;
    }

    /**
     * Campos de Lote (si tu tabla los tiene; si no, se ignoran).
     */
    public static Map<String, String> loteHeaderToSetter() {
        Map<String, String> m = new LinkedHashMap<>();

        m.put("Lote", "setLote");
        m.put("Objeto licitación/lote", "setObjetoLote");
        m.put("Valor estimado licitación/lote", "setValorEstimadoLote");
        m.put("Presupuesto base con impuestos licitación/lote", "setPresupuestoConImpuestosLote");
        m.put("Presupuesto base sin impuestos licitación/lote", "setPresupuestoSinImpuestosLote");
        m.put("CPV licitación/lote", "setCpvLote");
        m.put("Lugar ejecución licitación/lote", "setLugarEjecucionLote");

        return m;
    }

    /**
     * Campos de Resultado/Adjudicación (si tu tabla los tiene; si no, se ignoran).
     */
    public static Map<String, String> adjudicacionHeaderToSetter() {
        Map<String, String> m = new LinkedHashMap<>();

        m.put("Resultado licitación/lote", "setResultadoLote");
        m.put("Fecha del acuerdo licitación/lote", "setFechaAcuerdo");
        m.put("Número de ofertas recibidas por licitación/lote", "setNumeroOfertasRecibidas");

        m.put("Precio de la oferta más baja por licitación/lote", "setPrecioOfertaBaja");
        m.put("Precio de la oferta más alta por licitación/lote", "setPrecioOfertaAlta");
        m.put("Se han excluído ofertas por ser anormalmente bajas por licitación/lote", "setExcluidasPorBajaTemeraria");

        m.put("Número del contrato licitación/lote", "setNumeroContrato");
        m.put("Fecha formalización del contrato licitación/lote", "setFechaFormalizacion");
        m.put("Fecha entrada en vigor del contrato de licitación/lote", "setFechaEntradaVigor");

        m.put("Adjudicatario licitación/lote", "setAdjudicatarioNombre");
        m.put("Tipo de identificador de adjudicatario por licitación/lote", "setAdjudicatarioTipoId");
        m.put("Identificador Adjudicatario de la licitación/lote", "setAdjudicatarioId");
        m.put("El adjudicatario es o no PYME de la licitación/lote", "setAdjudicatarioEsPyme");

        m.put("Importe adjudicación sin impuestos licitación/lote", "setImporteAdjudicacionSinImp");
        m.put("Importe adjudicación con impuestos licitación/lote", "setImporteAdjudicacionConImp");

        return m;
    }
}

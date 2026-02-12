package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import com.ordenatec.portallicitaciones.infra.importacion.PlcspXlsxParser.PlcspRow;
import com.ordenatec.portallicitaciones.infra.persistence.entity.AdjudicacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PlcspAdjudicacionMapper {

    // Headers EXACTOS (según tu definición)
    private static final String H_ADJUDICATARIO = "Adjudicatario licitación/lote";
    private static final String H_IMPORTE_SIN_IMP = "Importe adjudicación sin impuestos licitación/lote";
    private static final String H_NUM_OFERTAS = "Número de ofertas recibidas por licitación/lote";
    private static final String H_FECHA_ACUERDO = "Fecha del acuerdo licitación/lote"; // (por ahora no lo usamos)

    public boolean hasAnyData(PlcspRow row) {
        if (row == null) return false;
        return notBlank(row.getTrim(H_ADJUDICATARIO))
                || row.getBigDecimal(H_IMPORTE_SIN_IMP) != null
                || notBlank(row.getTrim(H_NUM_OFERTAS))
                || row.getLocalDate(H_FECHA_ACUERDO) != null;
    }

    /**
     * Reglas:
     * - adjudicatario_nombre <- "Adjudicatario licitación/lote"
     * - importe_adjudicado <- columna importe sin impuestos (si viene); si no, fallback al precio_licitacion de la licitación
     * - numero_ofertas <- columna
     * - moneda = EUR
     * - uuid = uuid de licitación
     *
     * Campos derivados (estado, importe legacy, adjudicatario legacy) se completan en el Service.
     */
    public void mapInto(PlcspRow row, LicitacionEntity lic, AdjudicacionEntity a) {
        if (row == null || lic == null || a == null) return;

        // uuid desde licitación
        if (lic.getUuid() != null) {
            a.setUuid(lic.getUuid());
        }

        // adjudicatario_nombre
        String adj = row.getTrim(H_ADJUDICATARIO);
        if (notBlank(adj)) {
            a.setAdjudicatarioNombre(adj.trim());
        }

        // importe adjudicación (si viene)
        BigDecimal imp = row.getBigDecimal(H_IMPORTE_SIN_IMP);
        if (imp != null) {
            a.setImporteAdjudicado(imp);
        } else if (lic.getPrecioLicitacion() != null) {
            // fallback si no viene
            a.setImporteAdjudicado(lic.getPrecioLicitacion());
        }

        // numero ofertas
        String num = row.getTrim(H_NUM_OFERTAS);
        if (notBlank(num)) {
            try { a.setNumeroOfertas(Integer.parseInt(num.trim())); } catch (Exception ignored) {}
        }

        // moneda por defecto
        a.setMoneda("EUR");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}

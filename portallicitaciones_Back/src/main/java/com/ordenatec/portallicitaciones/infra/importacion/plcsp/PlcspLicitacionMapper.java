package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import com.ordenatec.portallicitaciones.infra.importacion.PlcspXlsxParser.PlcspRow;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class PlcspLicitacionMapper {

    // ============================
    // HEADERS REALES (TU XLSX)
    // ============================

    // TEXTO
    private static final String H_OBJETO_CONTRATO = "Objeto del Contrato";
    private static final String H_ESTADO = "Estado";
    private static final String H_TIPO_CONTRATO = "Tipo de contrato";
    private static final String H_TIPO_PROCEDIMIENTO = "Tipo de procedimiento"; // -> procedimiento
    private static final String H_TRAMITACION = "Tramitación";                  // -> modalidad (aprox)

    // LOCALIZACION
    // Tú quieres que "Lugar de ejecución" sea el CP -> en el XLSX viene como "Código Postal"
    private static final String H_CODIGO_POSTAL = "Código Postal";
    // (por si algún día quieres guardar el texto de lugar)
    private static final String H_LUGAR_EJECUCION_TEXTO = "Lugar de ejecución";

    // FECHAS
    private static final String H_FECHA_PUBLICACION = "Primera publicación";
    private static final String H_FECHA_LIMITE = "Fecha de presentación de ofertas";

    // IMPORTES
    private static final String H_PRECIO_LICITACION = "Presupuesto base sin impuestos";
    private static final String H_VALOR_ESTIMADO = "Valor estimado del contrato";

    // ORGANISMO TEXTO
    private static final String H_ORGANO = "Órgano de Contratación";

    public void mapIntoEntity(PlcspRow row, LicitacionEntity e) {
        if (row == null || e == null) return;

        // ===== TITULO =====
        // Objeto del Contrato -> titulo
        set(row, H_OBJETO_CONTRATO, e::setTitulo);

        // ===== ESTADO / TIPO / PROCEDIMIENTO =====
        set(row, H_ESTADO, e::setEstadoTexto);
        set(row, H_TIPO_CONTRATO, e::setTipoContratoTexto);

        // Tipo de procedimiento -> procedimiento
        set(row, H_TIPO_PROCEDIMIENTO, e::setProcedimiento);

        // Tramitación -> modalidad (es lo más parecido en tu modelo)
        set(row, H_TRAMITACION, e::setModalidad);

        // ===== LUGAR EJECUCION =====
        // Tú pediste: lugar_ejecucion = código postal
        set(row, H_CODIGO_POSTAL, e::setLugarEjecucion);

        // (Opcional) Si algún día prefieres guardar texto en lugar_ejecucion cuando no haya CP:
        // if (isBlank(e.getLugarEjecucion())) set(row, H_LUGAR_EJECUCION_TEXTO, e::setLugarEjecucion);

        // ===== FECHAS =====
        LocalDate fPub = row.getLocalDate(H_FECHA_PUBLICACION);
        if (fPub != null) e.setFechaPublicacion(fPub);

        LocalDate fLim = row.getLocalDate(H_FECHA_LIMITE);
        if (fLim != null) e.setFechaLimitePresentacion(fLim);

        // ===== IMPORTES =====
        BigDecimal precio = row.getBigDecimal(H_PRECIO_LICITACION);
        if (precio != null) e.setPrecioLicitacion(precio);

        BigDecimal valor = row.getBigDecimal(H_VALOR_ESTIMADO);
        if (valor != null) e.setValorEstimado(valor);

        // ===== MONEDA =====
        e.setMoneda("EUR");
        
    }

    // ---------------- Helpers ----------------

    private interface Setter {
        void apply(String value);
    }

    private static void set(PlcspRow row, String header, Setter setter) {
        String v = row.getTrim(header);
        if (v != null && !v.isBlank()) setter.apply(v.trim());
    }

    // (si lo quieres usar en el comentario opcional)
    // private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}

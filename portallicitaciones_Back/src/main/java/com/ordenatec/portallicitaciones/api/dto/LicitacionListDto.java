package com.ordenatec.portallicitaciones.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class LicitacionListDto {
    public UUID uuid;
    public String expediente;
    public String titulo;

    public String urlPublica;
    public String estadoTexto;

    public LocalDate fechaPublicacion;
    public LocalDate fechaLimitePresentacion;

    // 🔥 NUEVO: desde FK organismo_id (para que el buscador muestre lo mismo que el detalle)
    public Long organismoId;
    public String organismo;       // nombre
    public String organismoNif;    // nif

    // (los mantenemos por compatibilidad, por si algún front usa esto)
    public String entidad;
    public String organo;

    public String procedimiento;
    public String tipoContratoTexto;

    public BigDecimal precioLicitacion;
    public BigDecimal valorEstimado;
    public String moneda;

    public List<String> cpvs;
}

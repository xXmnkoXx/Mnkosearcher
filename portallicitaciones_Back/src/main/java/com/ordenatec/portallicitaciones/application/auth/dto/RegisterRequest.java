package com.ordenatec.portallicitaciones.application.auth.dto;

import java.math.BigDecimal;

public class RegisterRequest {

    // básicos
    public String username;
    public String email;
    public String password;

    // datos cliente/config (mapean a tu tabla usuarios)
    public Integer idCliente;
    public String nombreCliente;
    public String emailDestino;
    public String cpvs;

    public BigDecimal importeMax;
    public String tipoContrato;
    public String estado;

    public Integer desdeOffset;
    public Integer hastaOffset;
    public Integer maxPaginas;

    public String descripcion;
    public BigDecimal precio;

    public Boolean activo;
    public String rol;
}

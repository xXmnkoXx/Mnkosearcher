package com.ordenatec.portallicitaciones.application.auth.dto;

public class AuthResponse {
    public String token;
    public String username;
    public String rol;
    public Integer idUsuario;

    public AuthResponse(String token, String username, String rol, Integer idUsuario) {
        this.token = token;
        this.username = username;
        this.rol = rol;
        this.idUsuario = idUsuario;
    }
}

package com.ordenatec.portallicitaciones.application.auth;

import com.ordenatec.portallicitaciones.application.auth.dto.AuthResponse;
import com.ordenatec.portallicitaciones.application.auth.dto.LoginRequest;
import com.ordenatec.portallicitaciones.application.auth.dto.RegisterRequest;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UsuarioJpaRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UsuarioJpaRepository usuarios, PasswordEncoder encoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest req) {
        // básicos
        if (req == null) throw new IllegalArgumentException("body requerido");
        if (req.username == null || req.username.isBlank()) throw new IllegalArgumentException("username requerido");
        if (req.email == null || req.email.isBlank()) throw new IllegalArgumentException("email requerido");
        if (req.password == null || req.password.length() < 6) throw new IllegalArgumentException("password mínimo 6");

        String username = req.username.trim();
        String email = req.email.trim().toLowerCase();

        if (usuarios.existsByUsername(username)) throw new IllegalArgumentException("username ya existe");
        if (usuarios.existsByEmail(email)) throw new IllegalArgumentException("email ya existe");

        // defaults para NO NULL (tu tabla tiene NOT NULL en varios campos)
        Integer idCliente = (req.idCliente != null) ? req.idCliente : 0;

        String nombreCliente = (req.nombreCliente != null && !req.nombreCliente.isBlank())
                ? req.nombreCliente.trim()
                : username;

        // 👇 CLAVE: email_destino NOT NULL
        String emailDestino = (req.emailDestino != null && !req.emailDestino.isBlank())
                ? req.emailDestino.trim().toLowerCase()
                : email;

        UsuarioEntity u = new UsuarioEntity();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password));

        u.setRol((req.rol != null && !req.rol.isBlank()) ? req.rol.trim() : "USER");
        u.setActivo(req.activo != null ? req.activo : Boolean.TRUE);

        // cliente/config
        u.setIdCliente(idCliente);
        u.setNombreCliente(nombreCliente);
        u.setEmailDestino(emailDestino);

        u.setCpvs(req.cpvs != null ? req.cpvs : "");
        u.setImporteMax(req.importeMax != null ? req.importeMax : BigDecimal.ZERO);
        u.setTipoContrato(req.tipoContrato != null ? req.tipoContrato : "Servicios");
        u.setEstado(req.estado != null ? req.estado : "ACTIVO");
        u.setDesdeOffset(req.desdeOffset != null ? req.desdeOffset : 0);
        u.setHastaOffset(req.hastaOffset != null ? req.hastaOffset : 30);
        u.setMaxPaginas(req.maxPaginas != null ? req.maxPaginas : 30);
        u.setDescripcion(req.descripcion != null ? req.descripcion : "");
        u.setPrecio(req.precio != null ? req.precio : BigDecimal.ZERO);

        u.setFechaCreacion(LocalDateTime.now());
        u.setFechaUpdate(null);

        usuarios.save(u);
    }

    public AuthResponse login(LoginRequest req) {
        if (req == null) throw new IllegalArgumentException("body requerido");

        // IMPORTANTE: aquí usamos CAMPOS (req.username), no métodos (req.username())
        if (req.username == null || req.username.isBlank()) throw new IllegalArgumentException("username requerido");
        if (req.password == null || req.password.isBlank()) throw new IllegalArgumentException("password requerido");

        UsuarioEntity u = usuarios.findByUsername(req.username.trim())
                .orElseThrow(() -> new IllegalArgumentException("credenciales inválidas"));

        if (Boolean.FALSE.equals(u.getActivo())) throw new IllegalArgumentException("usuario inactivo");
        if (!encoder.matches(req.password, u.getPasswordHash()))
            throw new IllegalArgumentException("credenciales inválidas");

        String token = jwtService.createToken(u.getUsername(), u.getRol());
        return new AuthResponse(token, u.getUsername(), u.getRol(), u.getIdUsuario());
    }
}

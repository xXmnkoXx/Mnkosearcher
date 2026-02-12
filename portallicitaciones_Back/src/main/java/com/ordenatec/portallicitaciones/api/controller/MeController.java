package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UsuarioJpaRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public MeController(UsuarioJpaRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== DTOs mínimos (internos) =====
    public record MeProfileDTO(
            String username,
            String email,
            String telefono,
            String empresaNombre,
            String empresaCif
    ) {}

    public record UpdateMeDTO(String telefono) {}

    public record ChangePasswordDTO(String actual, String nueva) {}

    public record MePlanDTO(
            String suscripcion,
            BigDecimal importeAnual,
            String proximaFactura
    ) {}

    private UsuarioEntity currentUser(Authentication auth) {
        String username = auth.getName();
        return usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    @GetMapping
    public MeProfileDTO getMe(Authentication auth) {
        UsuarioEntity u = currentUser(auth);

        String empresaNombre = (u.getEmpresa() != null) ? u.getEmpresa().getNombre() : null;
        String empresaCif = (u.getEmpresa() != null) ? u.getEmpresa().getCif() : null;

        return new MeProfileDTO(
                u.getUsername(),
                u.getEmail(),
                u.getTelefono(),
                empresaNombre,
                empresaCif
        );
    }

    @PutMapping
    public void updateMe(@RequestBody UpdateMeDTO dto, Authentication auth) {
        UsuarioEntity u = currentUser(auth);

        // Solo editable: telefono (según tu UI)
        u.setTelefono(dto.telefono());
        usuarioRepo.save(u);
    }

    @PutMapping("/password")
    public void changePassword(@RequestBody ChangePasswordDTO dto, Authentication auth) {
        UsuarioEntity u = currentUser(auth);

        if (dto.actual() == null || dto.nueva() == null || dto.nueva().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña inválida");
        }

        if (!passwordEncoder.matches(dto.actual(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }

        u.setPasswordHash(passwordEncoder.encode(dto.nueva()));
        usuarioRepo.save(u);
    }

    @GetMapping("/plan")
    public MePlanDTO getPlan(Authentication auth) {
        UsuarioEntity u = currentUser(auth);

        // En tu tabla ya existe:
        // - suscripcion (String)
        // - precio (BigDecimal)
        // No tienes “próxima factura”, así que la calculo a 30 días.
        String proximaFactura = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE);

        return new MePlanDTO(
                u.getSuscripcion(),
                u.getPrecio(),
                proximaFactura
        );
    }
}

package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.admin.UsuarioService;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ============================
    // LISTAR (ADMIN)
    // ============================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioEntity>> listar() {

        List<UsuarioEntity> usuarios = usuarioService.listarUsuariosAdmin();
        return ResponseEntity.ok(usuarios);
    }

    // ============================
    // CREAR
    // ============================
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioEntity> crear(
            @RequestBody UsuarioCreateRequest req
    ) {

        UsuarioEntity nuevo = new UsuarioEntity();

        nuevo.setUsername(req.username());
        nuevo.setEmail(req.email());
        nuevo.setRol(req.rol());
        nuevo.setActivo(req.activo());
        nuevo.setSuscripcion(req.suscripcion());

        // aquí mandas password plano desde el front
        nuevo.setPasswordHash(req.password());

        UsuarioEntity creado =
                usuarioService.crearUsuario(nuevo, req.idEmpresa());

        return ResponseEntity.ok(creado);
    }

    // ============================
    // UPDATE
    // ============================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioEntity> actualizar(
            @PathVariable Integer id,
            @RequestBody UsuarioUpdateRequest req
    ) {

        UsuarioEntity cambios = new UsuarioEntity();

        cambios.setUsername(req.username());
        cambios.setEmail(req.email());
        cambios.setRol(req.rol());
        cambios.setActivo(req.activo());
        cambios.setSuscripcion(req.suscripcion());

        // opcional
        cambios.setPasswordHash(req.password());

        UsuarioEntity actualizado =
                usuarioService.actualizarUsuario(id, cambios, req.idEmpresa());

        return ResponseEntity.ok(actualizado);
    }

    // ============================
    // DELETE
    // ============================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {

        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    // ======================================================
    // DTOs internos (evitan exponer Entity directo)
    // ======================================================

    public record UsuarioCreateRequest(
            String username,
            String email,
            String password,
            String rol,
            Boolean activo,
            String suscripcion,
            Integer idEmpresa
    ) {}

    public record UsuarioUpdateRequest(
            String username,
            String email,
            String password,
            String rol,
            Boolean activo,
            String suscripcion,
            Integer idEmpresa
    ) {}
}

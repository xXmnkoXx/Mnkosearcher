package com.ordenatec.portallicitaciones.application.admin;

import com.ordenatec.portallicitaciones.infra.persistence.entity.EmpresaEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.EmpresaJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioJpaRepository usuarioRepo;
    private final EmpresaJpaRepository empresaRepo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioJpaRepository usuarioRepo,
            EmpresaJpaRepository empresaRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepo = usuarioRepo;
        this.empresaRepo = empresaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================
    // LISTADO ADMIN
    // ============================
    public List<UsuarioEntity> listarUsuariosAdmin() {
        // Usa JOIN FETCH para evitar Lazy error
        return usuarioRepo.findAllWithEmpresa();
    }

    // ============================
    // CREAR USUARIO
    // ============================
    public UsuarioEntity crearUsuario(UsuarioEntity input, Integer idEmpresa) {

        if (usuarioRepo.existsByUsername(input.getUsername())) {
            throw new RuntimeException("Username ya existe");
        }

        if (usuarioRepo.existsByEmail(input.getEmail())) {
            throw new RuntimeException("Email ya existe");
        }

        // Password hash
        input.setPasswordHash(passwordEncoder.encode(input.getPasswordHash()));

        // Empresa
        if (idEmpresa != null) {
            EmpresaEntity empresa = empresaRepo.findById(idEmpresa)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
            input.setEmpresa(empresa);
        }

        return usuarioRepo.save(input);
    }

    // ============================
    // ACTUALIZAR USUARIO
    // ============================
    public UsuarioEntity actualizarUsuario(Integer id, UsuarioEntity cambios, Integer idEmpresa) {

        UsuarioEntity db = usuarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        db.setUsername(cambios.getUsername());
        db.setEmail(cambios.getEmail());
        db.setRol(cambios.getRol());
        db.setActivo(cambios.getActivo());
        db.setSuscripcion(cambios.getSuscripcion());

        // Password solo si viene rellena
        if (cambios.getPasswordHash() != null && !cambios.getPasswordHash().isBlank()) {
            db.setPasswordHash(passwordEncoder.encode(cambios.getPasswordHash()));
        }

        // Empresa
        if (idEmpresa != null) {
            EmpresaEntity empresa = empresaRepo.findById(idEmpresa)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
            db.setEmpresa(empresa);
        } else {
            db.setEmpresa(null);
        }

        return usuarioRepo.save(db);
    }

    // ============================
    // BORRAR USUARIO
    // ============================
    public void eliminarUsuario(Integer id) {

        if (!usuarioRepo.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }

        usuarioRepo.deleteById(id);
    }
}

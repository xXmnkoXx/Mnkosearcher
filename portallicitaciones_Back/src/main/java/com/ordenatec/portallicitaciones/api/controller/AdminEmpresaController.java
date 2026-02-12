package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.infra.persistence.entity.EmpresaEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.EmpresaJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/empresas")
public class AdminEmpresaController {

    private final EmpresaJpaRepository empresaRepository;

    public AdminEmpresaController(EmpresaJpaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    // ==========================
    // LISTAR TODAS
    // ==========================

    @GetMapping
    public List<EmpresaEntity> listar() {
        return empresaRepository.findAll();
    }

    // ==========================
    // OBTENER POR ID
    // ==========================

    @GetMapping("/{id}")
    public EmpresaEntity obtener(@PathVariable Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
    }

    // ==========================
    // CREAR
    // ==========================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaEntity crear(@RequestBody EmpresaEntity empresa) {

        if (empresa.getNombre() == null || empresa.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la empresa es obligatorio");
        }

        empresa.setFechaCreacion(LocalDateTime.now());

        return empresaRepository.save(empresa);
    }

    // ==========================
    // ACTUALIZAR
    // ==========================

    @PutMapping("/{id}")
    public EmpresaEntity actualizar(
            @PathVariable Integer id,
            @RequestBody EmpresaEntity request
    ) {

        EmpresaEntity empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        empresa.setNombre(request.getNombre());
        empresa.setCif(request.getCif());
        empresa.setEmail(request.getEmail());
        empresa.setTelefono(request.getTelefono());
        empresa.setWeb(request.getWeb());
        empresa.setTamano(request.getTamano());
        empresa.setVia(request.getVia());
        empresa.setNumero(request.getNumero());
        empresa.setCodigoPostal(request.getCodigoPostal());
        empresa.setCiudad(request.getCiudad());
        empresa.setProvincia(request.getProvincia());

        return empresaRepository.save(empresa);
    }

    // ==========================
    // ELIMINAR
    // ==========================

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {

        if (!empresaRepository.existsById(id)) {
            return;
        }

        empresaRepository.deleteById(id);
    }
}

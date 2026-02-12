package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.infra.persistence.entity.AlertaEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.AlertaJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaJpaRepository alertas;
    private final UsuarioJpaRepository usuarios;

    public AlertaController(AlertaJpaRepository alertas, UsuarioJpaRepository usuarios) {
        this.alertas = alertas;
        this.usuarios = usuarios;
    }

    // =====================
    // GET /api/alertas/mias
    // =====================
    @GetMapping("/mias")
    public ResponseEntity<List<AlertaResponse>> listarMias(@AuthenticationPrincipal UserDetails principal) {
        UsuarioEntity u = currentUser(principal);
        List<AlertaEntity> rows = alertas.findByIdUsuarioOrderByIdAlertaDesc(u.getIdUsuario());
        return ResponseEntity.ok(rows.stream().map(AlertaResponse::from).toList());
    }

    // =====================
    // GET /api/alertas/{id}
    // =====================
    @GetMapping("/{id}")
    public ResponseEntity<AlertaResponse> getById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable("id") Long id) {

        UsuarioEntity u = currentUser(principal);

        AlertaEntity a = alertas.findByIdAlertaAndIdUsuario(id, u.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        return ResponseEntity.ok(AlertaResponse.from(a));
    }

    // =====================
    // POST /api/alertas
    // =====================
    @PostMapping
    public ResponseEntity<AlertaResponse> crear(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody AlertaRequest req) {

        UsuarioEntity u = currentUser(principal);

        AlertaEntity a = new AlertaEntity();
        a.setIdUsuario(u.getIdUsuario());
        a.setFechaCreacion(LocalDateTime.now());

        apply(req, a, true);

        AlertaEntity saved = alertas.save(a);
        return ResponseEntity.ok(AlertaResponse.from(saved));
    }

    // =====================
    // PUT /api/alertas/{id}
    // =====================
    @PutMapping("/{id}")
    public ResponseEntity<AlertaResponse> actualizar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable("id") Long id,
            @RequestBody AlertaRequest req) {

        UsuarioEntity u = currentUser(principal);

        AlertaEntity a = alertas.findByIdAlertaAndIdUsuario(id, u.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        apply(req, a, false);

        AlertaEntity saved = alertas.save(a);
        return ResponseEntity.ok(AlertaResponse.from(saved));
    }

    // =====================
    // DELETE /api/alertas/{id}
    // =====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable("id") Long id) {

        UsuarioEntity u = currentUser(principal);

        AlertaEntity a = alertas.findByIdAlertaAndIdUsuario(id, u.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        alertas.delete(a);
        return ResponseEntity.noContent().build();
    }

    // =====================
    // "Mapper" interno: request -> entity
    // =====================
    private void apply(AlertaRequest req, AlertaEntity a, boolean isCreate) {
        // ===== Campos base (los hacemos obligatorios en create) =====
        if (isCreate) {
            a.setCpvs(required(req.cpvs, "cpvs"));
            a.setTipo(required(req.tipo, "tipo"));
            // nombre obligatorio en create (recomendado)
            a.setNombre(required(req.nombre, "nombre"));
            // descripcion opcional: si no llega, la puedes rellenar con descripcionActividad o nombre
            a.setDescripcion(defaultIfBlank(req.descripcion, req.descripcionActividad, req.nombre, ""));
        } else {
            if (req.cpvs != null) a.setCpvs(req.cpvs.trim());
            if (req.tipo != null) a.setTipo(req.tipo.trim());
            if (req.nombre != null) a.setNombre(req.nombre.trim());
            if (req.descripcion != null) a.setDescripcion(req.descripcion.trim());
            else if (req.descripcionActividad != null && (a.getDescripcion() == null || a.getDescripcion().isBlank())) {
                a.setDescripcion(req.descripcionActividad.trim());
            }
        }

        // importes/activa (en update si viene null dejamos lo existente)
        if (req.importeMin != null || isCreate) a.setImporteMin(req.importeMin);
        if (req.importeMax != null || isCreate) a.setImporteMax(req.importeMax);
        if (req.activa != null) a.setActiva(req.activa);
        else if (isCreate && a.getActiva() == null) a.setActiva(Boolean.TRUE);

        // ===== PARÁMETROS =====
        if (req.palabrasClave != null || isCreate) a.setPalabrasClave(trimOrNull(req.palabrasClave));
        if (req.descripcionActividad != null || isCreate) a.setDescripcionActividad(trimOrNull(req.descripcionActividad));
        if (req.tiposContratos != null || isCreate) a.setTiposContratos(trimOrNull(req.tiposContratos));
        if (req.contratosMenores != null) a.setContratosMenores(req.contratosMenores);
        else if (isCreate && a.getContratosMenores() == null) a.setContratosMenores(Boolean.FALSE);

        if (req.lugares != null || isCreate) a.setLugares(trimOrNull(req.lugares));
        if (req.organosContratacion != null || isCreate) a.setOrganosContratacion(trimOrNull(req.organosContratacion));

        // ===== ENVÍO =====
        if (req.frecuenciaEnvio != null || isCreate) a.setFrecuenciaEnvio(trimOrNull(req.frecuenciaEnvio));
        if (req.horaEnvio != null || isCreate) a.setHoraEnvio(parseHora(req.horaEnvio));
        if (req.diaSemana != null || isCreate) a.setDiaSemana(trimOrNull(req.diaSemana));

        if (req.notificarCambios != null) a.setNotificarCambios(req.notificarCambios);
        else if (isCreate && a.getNotificarCambios() == null) a.setNotificarCambios(Boolean.TRUE);

        if (req.email1 != null || isCreate) a.setEmail1(trimOrNull(req.email1));
        if (req.email2 != null || isCreate) a.setEmail2(trimOrNull(req.email2));
        if (req.email3 != null || isCreate) a.setEmail3(trimOrNull(req.email3));
    }

    private LocalTime parseHora(String hora) {
        if (hora == null || hora.isBlank()) return null;
        return LocalTime.parse(hora.trim()); // "10:00" o "10:00:00"
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String defaultIfBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }

    // =====================
    // Helpers auth/validación
    // =====================
    private UsuarioEntity currentUser(UserDetails principal) {
        if (principal == null || principal.getUsername() == null) {
            throw new IllegalArgumentException("No autenticado");
        }
        return usuarios.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private String required(String v, String field) {
        if (v == null || v.isBlank())
            throw new IllegalArgumentException(field + " requerido");
        return v.trim();
    }

    // =====================
    // DTOs internos (ampliados)
    // =====================
    public static class AlertaRequest {
        // base
        public String nombre;              // ✅ NUEVO
        public String cpvs;
        public String descripcion;         // resumen (opcional)
        public String tipo;                // "SERVICIOS" | "SUMINISTRO"
        public BigDecimal importeMin;
        public BigDecimal importeMax;
        public Boolean activa;

        // parámetros
        public String palabrasClave;
        public String descripcionActividad;
        public String tiposContratos;
        public Boolean contratosMenores;
        public String lugares;
        public String organosContratacion;

        // envío
        public String frecuenciaEnvio;     // DIARIAMENTE | SEMANALMENTE | INMEDIATO
        public String horaEnvio;           // "10:00"
        public String diaSemana;           // Lunes..Domingo
        public Boolean notificarCambios;
        public String email1;
        public String email2;
        public String email3;
    }

    public static class AlertaResponse {
        public Long idAlerta;
        public Integer idUsuario;

        public String nombre;              // ✅ NUEVO

        public String cpvs;
        public String descripcion;
        public String tipo;
        public BigDecimal importeMin;
        public BigDecimal importeMax;
        public Boolean activa;
        public LocalDateTime fechaCreacion;

        // parámetros
        public String palabrasClave;
        public String descripcionActividad;
        public String tiposContratos;
        public Boolean contratosMenores;
        public String lugares;
        public String organosContratacion;

        // envío
        public String frecuenciaEnvio;
        public String horaEnvio;
        public String diaSemana;
        public Boolean notificarCambios;
        public String email1;
        public String email2;
        public String email3;

        public static AlertaResponse from(AlertaEntity e) {
            AlertaResponse r = new AlertaResponse();
            r.idAlerta = e.getIdAlerta();
            r.idUsuario = e.getIdUsuario();

            r.nombre = e.getNombre();

            r.cpvs = e.getCpvs();
            r.descripcion = e.getDescripcion();
            r.tipo = e.getTipo();
            r.importeMin = e.getImporteMin();
            r.importeMax = e.getImporteMax();
            r.activa = e.getActiva();
            r.fechaCreacion = e.getFechaCreacion();

            r.palabrasClave = e.getPalabrasClave();
            r.descripcionActividad = e.getDescripcionActividad();
            r.tiposContratos = e.getTiposContratos();
            r.contratosMenores = e.getContratosMenores();
            r.lugares = e.getLugares();
            r.organosContratacion = e.getOrganosContratacion();

            r.frecuenciaEnvio = e.getFrecuenciaEnvio();
            r.horaEnvio = e.getHoraEnvio() != null ? e.getHoraEnvio().toString() : null;
            r.diaSemana = e.getDiaSemana();
            r.notificarCambios = e.getNotificarCambios();
            r.email1 = e.getEmail1();
            r.email2 = e.getEmail2();
            r.email3 = e.getEmail3();

            return r;
        }
    }
}

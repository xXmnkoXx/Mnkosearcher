package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioLicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioLicitacionJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me")
public class MisLicitacionesEnviadasController {

    private final UsuarioJpaRepository usuarioRepo;
    private final UsuarioLicitacionJpaRepository usuarioLicitRepo;
    private final LicitacionJpaRepository licitacionRepo;

    public MisLicitacionesEnviadasController(
            UsuarioJpaRepository usuarioRepo,
            UsuarioLicitacionJpaRepository usuarioLicitRepo,
            LicitacionJpaRepository licitacionRepo
    ) {
        this.usuarioRepo = usuarioRepo;
        this.usuarioLicitRepo = usuarioLicitRepo;
        this.licitacionRepo = licitacionRepo;
    }

    /**
     * Histórico completo (paginado) de lo enviado al usuario (usuario_licitaciones).
     *
     * GET /api/me/licitaciones-enviadas?page=0&size=50&tipo=LICIT|MENOR&q=texto
     */
    @GetMapping("/licitaciones-enviadas")
    public ResponseEntity<?> listar(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "50") int size,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "q", required = false) String q,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "error", "No autenticado"));
        }

        String username = principal.getName();
        Optional<UsuarioEntity> optUser = usuarioRepo.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "error", "Usuario no encontrado", "username", username));
        }

        UsuarioEntity user = optUser.get();

        PageRequest pr = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "fechaCreacion")
        );

        String tipoNorm = (tipo == null || tipo.isBlank()) ? null : tipo.trim().toUpperCase();
        String qNorm = (q == null || q.isBlank()) ? null : q.trim();

        Page<UsuarioLicitacionEntity> result;
        if (tipoNorm == null && qNorm == null) {
            result = usuarioLicitRepo.pageByUsuario(user.getIdUsuario(), pr);
        } else if (tipoNorm != null && qNorm == null) {
            result = usuarioLicitRepo.pageByUsuarioAndTipo(user.getIdUsuario(), tipoNorm, pr);
        } else if (tipoNorm == null) {
            result = usuarioLicitRepo.pageByUsuarioAndQuery(user.getIdUsuario(), qNorm, pr);
        } else {
            result = usuarioLicitRepo.pageByUsuarioTipoAndQuery(user.getIdUsuario(), tipoNorm, qNorm, pr);
        }

        // ✅ Mapear expediente (id_externo) -> LicitacionEntity (uuid + organismo real)
        List<String> expedientes = result.getContent().stream()
                .map(UsuarioLicitacionEntity::getIdExterno)  // en tu tabla es el expediente
                .map(this::safeTrim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        final Map<String, LicitacionEntity> expToLicit = expedientes.isEmpty()
                ? Map.of()
                : licitacionRepo.findAllByExpedienteIn(expedientes).stream()
                    .filter(l -> l.getExpediente() != null)
                    .collect(Collectors.toMap(
                            l -> safeTrim(l.getExpediente()),
                            l -> l,
                            (a, b) -> a
                    ));

        List<ItemDto> items = result.getContent().stream()
                .map(e -> ItemDto.from(e, expToLicit.get(safeTrim(e.getIdExterno()))))
                .toList();

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "items", items
        ));
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    // ---------------- DTO salida ----------------

    public static class ItemDto {
        public Integer id;
        public String tipo;
        public String id_externo;        // expediente guardado en usuario_licitaciones
        public String uuid;              // uuid real de licitaciones (si existe)

        public String titulo;
        public String organismo;         // lo mostramos ya como nombre real si existe
        public String organismo_nif;     // nif del organismo si existe

        public String fecha_limite;
        public BigDecimal importe;
        public String enlace;
        public String cpv;
        public String fuente;
        public String fecha_envio;
        public String fecha_creacion;

        public static ItemDto from(UsuarioLicitacionEntity e, LicitacionEntity licit) {
            ItemDto d = new ItemDto();
            d.id = e.getIdUsuarioLicitacion();
            d.tipo = e.getTipo();
            d.id_externo = e.getIdExterno();

            // uuid + organismo desde licitaciones (si existe)
            d.uuid = (licit != null && licit.getUuid() != null) ? licit.getUuid().toString() : null;

            // Título: preferimos el de licitaciones si existe
            d.titulo = (licit != null && licit.getTitulo() != null) ? licit.getTitulo() : e.getTitulo();

            // Organismo: nombre real si existe, si no el string guardado ("Sector Público"...)
            if (licit != null && licit.getOrganismo() != null) {
                d.organismo = licit.getOrganismo().getNombre();
                d.organismo_nif = licit.getOrganismo().getNif();
            } else {
                d.organismo = e.getOrganismo();
                d.organismo_nif = null;
            }

            d.fecha_limite = (e.getFechaLimite() == null) ? null : e.getFechaLimite().toString();
            d.importe = e.getImporte();
            d.enlace = e.getEnlace();
            d.cpv = e.getCpv();
            d.fuente = e.getFuente();
            d.fecha_envio = (e.getFechaEnvio() == null) ? null : e.getFechaEnvio().toString();
            d.fecha_creacion = (e.getFechaCreacion() == null) ? null : e.getFechaCreacion().toString();
            return d;
        }
    }
}

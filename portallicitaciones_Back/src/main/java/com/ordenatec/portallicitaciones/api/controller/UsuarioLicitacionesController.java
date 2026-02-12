package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioLicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioLicitacionJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioLicitacionesController {

    private final UsuarioJpaRepository usuarioRepo;
    private final UsuarioLicitacionJpaRepository usuarioLicitRepo;

    public UsuarioLicitacionesController(UsuarioJpaRepository usuarioRepo,
                                         UsuarioLicitacionJpaRepository usuarioLicitRepo) {
        this.usuarioRepo = usuarioRepo;
        this.usuarioLicitRepo = usuarioLicitRepo;
    }

    /**
     * MIS LICITACIONES (por usuario):
     * GET /api/usuarios/{idUsuario}/licitaciones?page=0&size=50&tipo=LICIT|MENOR&q=texto
     *
     * Devuelve SOLO lo que está en usuario_licitaciones para ese usuario.
     */
    @GetMapping("/{idUsuario}/licitaciones")
    public ResponseEntity<?> listarPorUsuario(
            @PathVariable Integer idUsuario,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "q", required = false) String q
    ) {
        Optional<UsuarioEntity> opt = usuarioRepo.findById(idUsuario);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "ok", false,
                    "error", "Usuario no encontrado",
                    "idUsuario", idUsuario
            ));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        PageRequest pr = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "fechaCreacion")
        );

        String tipoNorm = (tipo == null || tipo.isBlank()) ? null : tipo.trim().toUpperCase();
        String qNorm = (q == null || q.isBlank()) ? null : q.trim();

        Page<UsuarioLicitacionEntity> result;

        // OJO: estos métodos los crearemos en la siguiente clase (repositorio)
        if (tipoNorm == null && qNorm == null) {
            result = usuarioLicitRepo.pageByUsuario(idUsuario, pr);
        } else if (tipoNorm != null && qNorm == null) {
            result = usuarioLicitRepo.pageByUsuarioAndTipo(idUsuario, tipoNorm, pr);
        } else if (tipoNorm == null) {
            result = usuarioLicitRepo.pageByUsuarioAndQuery(idUsuario, qNorm, pr);
        } else {
            result = usuarioLicitRepo.pageByUsuarioTipoAndQuery(idUsuario, tipoNorm, qNorm, pr);
        }

        List<ItemDto> items = result.getContent().stream().map(ItemDto::from).toList();

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "idUsuario", idUsuario,
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "items", items
        ));
    }

    // ===== DTO de salida (lo que consumirá el front en "Mis licitaciones") =====
    public static class ItemDto {
        public Integer id;
        public String tipo;
        public String id_externo;
        public String titulo;
        public String organismo;
        public String fecha_limite;
        public BigDecimal importe;
        public String enlace;
        public String cpv;
        public String fuente;
        public String fecha_envio;
        public String fecha_creacion;

        public static ItemDto from(UsuarioLicitacionEntity e) {
            ItemDto d = new ItemDto();
            d.id = e.getIdUsuarioLicitacion();
            d.tipo = e.getTipo();
            d.id_externo = e.getIdExterno();
            d.titulo = e.getTitulo();
            d.organismo = e.getOrganismo();
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

package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.api.dto.LicitacionFullDto;
import com.ordenatec.portallicitaciones.api.dto.LicitacionListDto;
import com.ordenatec.portallicitaciones.api.dto.PublicadasResponse;
import com.ordenatec.portallicitaciones.application.service.LicitacionService;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/licitaciones")
public class LicitacionController {

    private final LicitacionService licitacionService;

    public LicitacionController(LicitacionService licitacionService) {
        this.licitacionService = licitacionService;
    }

    /**
     * ✅ NO ROMPER FRONT:
     * GET /api/licitaciones
     * Devuelve el listado "viejo" (dominio) tal como estaba.
     */
    @GetMapping
    public List<Licitacion> listar() {
        return licitacionService.listar();
    }

    /**
     * ✅ Endpoint NUEVO (si quieres usar DTO con organismo, etc.)
     * GET /api/licitaciones/list
     */
    @GetMapping("/list")
    public List<LicitacionListDto> listarDto() {
        return licitacionService.listarResumen();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LicitacionFullDto> detalle(@PathVariable UUID id) {
        return licitacionService.obtenerDetalleFull(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ PUBLICO para n8n (y también te sirve para buscador si migras el front)
    @PreAuthorize("permitAll()")
    @GetMapping("/publicadas")
    public PublicadasResponse<LicitacionListDto> publicadasFiltradas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false, name = "from") LocalDate fromDate,
            @RequestParam(required = false) String cpvs,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size,
            @RequestParam Map<String, String> allParams
    ) {
        Page<LicitacionListDto> result =
                licitacionService.listarPublicadasFiltradas(estado, fromDate, cpvs, page, size);

        Map<String, Object> params = buildClientParams(allParams);

        params.putIfAbsent("estado", estado);
        params.putIfAbsent("from", fromDate != null ? fromDate.toString() : null);
        params.putIfAbsent("cpvs", cpvs);
        params.putIfAbsent("page", page);
        params.putIfAbsent("size", size);

        return new PublicadasResponse<>(params, result);
    }

    private Map<String, Object> buildClientParams(Map<String, String> qp) {
        Map<String, Object> p = new LinkedHashMap<>();

        String clienteId =
                firstNonBlank(qp.get("cliente_id"), qp.get("id_cliente"), qp.get("clienteId"), qp.get("idCliente"));

        String clienteNombre =
                firstNonBlank(qp.get("cliente_nombre"), qp.get("nombre_cliente"), qp.get("clienteNombre"), qp.get("nombreCliente"));

        String clienteEmail =
                firstNonBlank(qp.get("cliente_email"), qp.get("email_destino"), qp.get("email"), qp.get("to"));

        String to =
                firstNonBlank(qp.get("__to"), qp.get("to"), qp.get("email_destino"), qp.get("cliente_email"), qp.get("email"));

        if (clienteId != null) p.put("cliente_id", clienteId);
        if (clienteNombre != null) p.put("cliente_nombre", clienteNombre);
        if (clienteEmail != null) p.put("cliente_email", clienteEmail);
        if (to != null) p.put("__to", to);

        return p;
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null) {
                String s = v.trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }
}

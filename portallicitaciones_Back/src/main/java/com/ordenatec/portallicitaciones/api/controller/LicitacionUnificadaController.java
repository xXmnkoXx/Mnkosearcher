package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.api.dto.LicitacionUnificadaDto;
import com.ordenatec.portallicitaciones.api.dto.PublicadasResponse;
import com.ordenatec.portallicitaciones.application.service.LicitacionUnificadaService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/licitaciones")
public class LicitacionUnificadaController {

    private final LicitacionUnificadaService service;

    public LicitacionUnificadaController(LicitacionUnificadaService service) {
        this.service = service;
    }

    /**
     * ✅ ROUTE: GET /api/licitaciones/unificadas
     *
     * Params:
     * - source = mix | nacional | catalunya (default: mix)
     * - q = texto libre (opcional)
     * - page = 0..n (default: 0)
     * - size = 1..500 (default: 200)
     */
    @PreAuthorize("permitAll()")
    @GetMapping("/unificadas")
    public PublicadasResponse<LicitacionUnificadaDto> unificadas(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "all") String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        Page<LicitacionUnificadaDto> result = service.listarUnificadas(source, q, estado, page, size);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("source", source == null ? "mix" : source);
        params.put("q", q);
        params.put("estado", estado);
        params.put("page", page);
        params.put("size", size);

        return new PublicadasResponse<>(params, result);
    }

}

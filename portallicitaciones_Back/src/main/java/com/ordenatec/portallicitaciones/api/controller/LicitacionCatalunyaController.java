package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionCatalunyaEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionCatalunyaJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/licitaciones")
public class LicitacionCatalunyaController {

    private final LicitacionCatalunyaJpaRepository catalunyaRepo;

    public LicitacionCatalunyaController(LicitacionCatalunyaJpaRepository catalunyaRepo) {
        this.catalunyaRepo = catalunyaRepo;
    }

    /**
     * GET /api/licitaciones/catalunya?page=0&size=50&q=texto
     */
    @GetMapping("/catalunya")
    public Page<LicitacionCatalunyaEntity> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String q
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 500);

        Pageable p = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "dataPublicacioAnunci")
        );

        String query = (q == null) ? "" : q.trim();
        if (query.isEmpty()) {
            return catalunyaRepo.findAll(p);
        }

        return catalunyaRepo.findByCodiExpedientContainingIgnoreCaseOrDenominacioContainingIgnoreCaseOrNomOrganContainingIgnoreCase(
                query, query, query, p
        );
    }
}

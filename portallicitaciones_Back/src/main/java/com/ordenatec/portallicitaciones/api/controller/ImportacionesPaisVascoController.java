package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.service.ImportacionEuskadiService;
import com.ordenatec.portallicitaciones.application.usecase.ImportarEuskadiDiarioUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/importaciones/pais-vasco")
public class ImportacionesPaisVascoController {

    private final ImportacionEuskadiService importacionEuskadiService;

    public ImportacionesPaisVascoController(ImportacionEuskadiService importacionEuskadiService) {
        this.importacionEuskadiService = importacionEuskadiService;
    }

    /**
     * Importación diaria País Vasco (Euskadi)
     * POST /api/importaciones/pais-vasco/diario
     *
     * daysBack:
     *  - por defecto = 1 (import diario real)
     *  - se puede subir manualmente si se desea (ej: ?daysBack=7)
     */
    @PostMapping("/diario")
    public ResponseEntity<Map<String, Object>> importarDiario(
            @RequestParam(name = "daysBack", defaultValue = "1") int daysBack
    ) {
        ImportarEuskadiDiarioUseCase.Resultado r =
                importacionEuskadiService.importarDiario(daysBack);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "paginasProcesadas", r.paginasProcesadas(),
                "noticesLeidos", r.noticesLeidos(),
                "noticesEnriquecidos", r.noticesEnriquecidos(),
                "licitacionesUpsert", r.licitacionesUpsert()
        ));
    }

    /**
     * Alias:
     * POST /api/importaciones/pais-vasco
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> importarDiarioAlias(
            @RequestParam(name = "daysBack", defaultValue = "1") int daysBack
    ) {
        return importarDiario(daysBack);
    }
}

package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.usecase.ImportarNavarraDiarioUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint para lanzar la importación diaria de Navarra (Datos Abiertos - datastore dump).
 */
@RestController
@RequestMapping("/api/importaciones/navarra")
public class ImportacionesNavarraController {

    private final ImportarNavarraDiarioUseCase useCase;

    public ImportacionesNavarraController(ImportarNavarraDiarioUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * POST /api/importaciones/navarra/diario?daysBack=7
     */
    @PostMapping("/diario")
    public ResponseEntity<ImportarNavarraDiarioUseCase.Resultado> importarDiario(
            @RequestParam(name = "daysBack", required = false, defaultValue = "1") int daysBack
    ) {
        return ResponseEntity.ok(useCase.ejecutar(daysBack));
    }

    /**
     * Alias: POST /api/importaciones/navarra?daysBack=7
     */
    @PostMapping
    public ResponseEntity<ImportarNavarraDiarioUseCase.Resultado> importarDiarioAlias(
            @RequestParam(name = "daysBack", required = false, defaultValue = "1") int daysBack
    ) {
        return ResponseEntity.ok(useCase.ejecutar(daysBack));
    }
}

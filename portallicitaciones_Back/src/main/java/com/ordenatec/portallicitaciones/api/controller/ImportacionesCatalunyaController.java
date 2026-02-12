package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.usecase.ImportarCatalunyaDiarioUseCase;
import com.ordenatec.portallicitaciones.application.usecase.EnriquecerDetalleCatalunyaUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de importación y enriquecimiento de Cataluña (PSCP).
 */
@RestController
@RequestMapping("/api/importaciones/catalunya")
public class ImportacionesCatalunyaController {

    private final ImportarCatalunyaDiarioUseCase importarDiarioUseCase;
    private final EnriquecerDetalleCatalunyaUseCase enriquecerDetalleUseCase;

    public ImportacionesCatalunyaController(
            ImportarCatalunyaDiarioUseCase importarDiarioUseCase,
            EnriquecerDetalleCatalunyaUseCase enriquecerDetalleUseCase
    ) {
        this.importarDiarioUseCase = importarDiarioUseCase;
        this.enriquecerDetalleUseCase = enriquecerDetalleUseCase;
    }

    /**
     * Importación diaria de Cataluña (dataset abierto).
     *
     * POST /api/importaciones/catalunya/diario?daysBack=1
     */
    @PostMapping("/diario")
    public ResponseEntity<ImportarCatalunyaDiarioUseCase.Resultado> importarDiario(
            @RequestParam(name = "daysBack", required = false, defaultValue = "1") int daysBack
    ) {
        return ResponseEntity.ok(importarDiarioUseCase.ejecutar(daysBack));
    }

    /**
     * Alias del diario:
     * POST /api/importaciones/catalunya?daysBack=1
     */
    @PostMapping
    public ResponseEntity<ImportarCatalunyaDiarioUseCase.Resultado> importarDiarioAlias(
            @RequestParam(name = "daysBack", required = false, defaultValue = "1") int daysBack
    ) {
        return ResponseEntity.ok(importarDiarioUseCase.ejecutar(daysBack));
    }

    /**
     * Enriquecimiento SOLO del detalle (HTML PSCP).
     *
     * POST /api/importaciones/catalunya/detalle/enriquecer?daysBack=60&force=false
     */
    @PostMapping("/detalle/enriquecer")
    public ResponseEntity<EnriquecerDetalleCatalunyaUseCase.Resultado> enriquecerDetalle(
            @RequestParam(name = "daysBack", required = false, defaultValue = "30") int daysBack,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force
    ) {
        return ResponseEntity.ok(
                enriquecerDetalleUseCase.ejecutar(daysBack, force)
        );
    }

    /**
     * Importación diaria + enriquecimiento de detalle (recomendado).
     *
     * POST /api/importaciones/catalunya/diario-completo?daysBack=60&force=false
     */
    @PostMapping("/diario-completo")
    public ResponseEntity<?> importarDiarioCompleto(
            @RequestParam(name = "daysBack", required = false, defaultValue = "30") int daysBack,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force
    ) {
        var importacion = importarDiarioUseCase.ejecutar(daysBack);
        var detalle = enriquecerDetalleUseCase.ejecutar(daysBack, force);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "importacion", importacion,
                        "detalle", detalle
                )
        );
    }
}

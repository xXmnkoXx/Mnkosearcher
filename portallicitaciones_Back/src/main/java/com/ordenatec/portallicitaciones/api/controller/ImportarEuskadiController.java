package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.usecase.ImportarEuskadiDiarioUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/importaciones/euskadi")
public class ImportarEuskadiController {

    private final ImportarEuskadiDiarioUseCase importarEuskadiDiarioUseCase;

    public ImportarEuskadiController(ImportarEuskadiDiarioUseCase importarEuskadiDiarioUseCase) {
        this.importarEuskadiDiarioUseCase = importarEuskadiDiarioUseCase;
    }

    /**
     * Importación diaria Euskadi (feed/listado)
     *
     * Ejemplos:
     *  POST /api/importaciones/euskadi/diario
     *  POST /api/importaciones/euskadi/diario?daysBack=3
     */
    @PostMapping("/diario")
    public ResponseEntity<ImportarEuskadiDiarioUseCase.Resultado> importarDiario(
            @RequestParam(name = "daysBack", required = false, defaultValue = "1") int daysBack
    ) {
        ImportarEuskadiDiarioUseCase.Resultado resultado =
                importarEuskadiDiarioUseCase.ejecutar(daysBack);

        return ResponseEntity.ok(resultado);
    }
}

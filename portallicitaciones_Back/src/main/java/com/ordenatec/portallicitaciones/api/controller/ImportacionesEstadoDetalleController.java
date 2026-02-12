package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.usecase.EnriquecerDetalleEstadoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/importaciones/euskadi/detalle")
public class ImportacionesEstadoDetalleController {

    private final EnriquecerDetalleEstadoUseCase enriquecerDetalleEstadoUseCase;

    public ImportacionesEstadoDetalleController(EnriquecerDetalleEstadoUseCase enriquecerDetalleEstadoUseCase) {
        this.enriquecerDetalleEstadoUseCase = enriquecerDetalleEstadoUseCase;
    }

    /**
     * Enriquecer "estado" bajando el HTML de la url_publica y extrayendo estadoTexto.
     *
     * Ejemplos:
     *  POST /api/importaciones/euskadi/detalle/enriquecer?daysBack=60&force=true
     *  POST /api/importaciones/euskadi/detalle/enriquecer?daysBack=7
     */
    @PostMapping("/enriquecer")
    public ResponseEntity<EnriquecerDetalleEstadoUseCase.Resultado> enriquecer(
            @RequestParam(name = "daysBack", required = false, defaultValue = "30") int daysBack,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force
    ) {
        EnriquecerDetalleEstadoUseCase.Resultado res = enriquecerDetalleEstadoUseCase.ejecutar(daysBack, force);
        return ResponseEntity.ok(res);
    }
}

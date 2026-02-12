package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.usecase.ImportarCatalunyaNdjsonUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/importaciones/catalunya")
public class ImportacionesCatalunyaNdjsonController {

    // ✅ Ruta fija RELATIVA al proyecto (NO varía)
    // Generada por /api/importaciones/python-export/catalunya/download
    private static final Path FIXED_FILE =
            Paths.get("importsPython", "catalunya.ndjson.gz").toAbsolutePath().normalize();

    private final ImportarCatalunyaNdjsonUseCase useCase;

    public ImportacionesCatalunyaNdjsonController(ImportarCatalunyaNdjsonUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Importa SIEMPRE ./importsPython/catalunya.ndjson.gz a dbo.licitacionesCatalunya
     *
     * POST /api/importaciones/catalunya/ndjson?reset=true&batchSize=1000
     */
    @PostMapping("/ndjson")
    public ResponseEntity<?> importar(
            @RequestParam(name = "reset", defaultValue = "false") boolean reset,
            @RequestParam(name = "batchSize", defaultValue = "1000") int batchSize
    ) throws IOException, SQLException {

        if (!Files.exists(FIXED_FILE)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "No existe el fichero en la ruta fija",
                    "file", FIXED_FILE.toString(),
                    "hint", "Primero ejecuta: POST /api/importaciones/python-export/catalunya/download"
            ));
        }

        var r = useCase.ejecutar(FIXED_FILE, reset, batchSize);
        return ResponseEntity.ok(r);
    }
}

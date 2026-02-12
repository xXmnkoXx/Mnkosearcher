package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.usecase.ImportarNacionalNdjsonUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/importaciones/nacional")
public class ImportacionesNacionalNdjsonController {

    private static final Path FIXED_FILE =
            Paths.get("importsPython", "nacional.ndjson.gz").toAbsolutePath().normalize();

    private final ImportarNacionalNdjsonUseCase importarNacionalNdjsonUseCase;

    public ImportacionesNacionalNdjsonController(ImportarNacionalNdjsonUseCase importarNacionalNdjsonUseCase) {
        this.importarNacionalNdjsonUseCase = importarNacionalNdjsonUseCase;
    }

    /**
     * Importa SIEMPRE ./importsPython/nacional.ndjson.gz a dbo.licitacionesNacional
     *
     * POST /api/importaciones/nacional/ndjson?reset=true&batchSize=1000
     */
    @PostMapping("/ndjson")
    public ResponseEntity<?> importarNdjson(
            @RequestParam(name = "reset", defaultValue = "false") boolean reset,
            @RequestParam(name = "batchSize", defaultValue = "1000") int batchSize
    ) throws IOException, SQLException {

        if (!Files.exists(FIXED_FILE)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "No existe el fichero en la ruta fija",
                    "file", FIXED_FILE.toString(),
                    "hint", "Asegúrate de arrancar el back desde D:\\portalLicitaciones\\portallicitaciones_Back"
            ));
        }

        ImportarNacionalNdjsonUseCase.Resultado r =
                importarNacionalNdjsonUseCase.ejecutar(FIXED_FILE.toString(), reset, batchSize);

        return ResponseEntity.ok(r);
    }

    /**
     * Variante por subida de fichero (multipart)
     *
     * POST /api/importaciones/nacional/ndjson/upload?reset=true&batchSize=1000
     * form-data: file=@nacional.ndjson.gz
     */
    @PostMapping(value = "/ndjson/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importarNdjsonUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "reset", defaultValue = "false") boolean reset,
            @RequestParam(name = "batchSize", defaultValue = "1000") int batchSize
    ) throws IOException, SQLException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Fichero vacío"));
        }

        ImportarNacionalNdjsonUseCase.Resultado r = importarNacionalNdjsonUseCase.ejecutar(
                file.getInputStream(),
                "upload:" + file.getOriginalFilename(),
                reset,
                batchSize
        );

        return ResponseEntity.ok(r);
    }
}

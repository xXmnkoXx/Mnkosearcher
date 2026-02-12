package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.application.usecase.DescargarPythonExportUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/importaciones/python-export")
public class ImportacionesPythonExportController {

    private final DescargarPythonExportUseCase useCase;
    private final Path downloadDir;

    public ImportacionesPythonExportController(
            DescargarPythonExportUseCase useCase,
            @Value("${python.export.download-dir:work/python-exports}") String downloadDir
    ) {
        this.useCase = useCase;
        this.downloadDir = Paths.get(downloadDir).toAbsolutePath().normalize();
    }

    /**
     * POST /api/importaciones/python-export/nacional/download?yearMin=2026&limit=0&batchRows=50000
     */
    @PostMapping("/nacional/download")
    public ResponseEntity<DescargarPythonExportUseCase.Resultado> descargarNacional(
            @RequestParam int yearMin,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer batchRows
    ) {
        return ResponseEntity.ok(useCase.descargarNacional(yearMin, limit, batchRows));
    }

    /**
     * POST /api/importaciones/python-export/catalunya/download?yearMin=2026&yearMax=2026&datasets=publicaciones_pscp&limit=0
     */
    @PostMapping("/catalunya/download")
    public ResponseEntity<DescargarPythonExportUseCase.Resultado> descargarCatalunya(
            @RequestParam int yearMin,
            @RequestParam int yearMax,
            @RequestParam(required = false) String datasets,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer batchRows
    ) {
        return ResponseEntity.ok(useCase.descargarCatalunya(yearMin, yearMax, datasets, limit, batchRows));
    }

    /**
     * GET /api/importaciones/python-export/files/{fileName}
     * Descarga el .ndjson.gz generado
     */
    @GetMapping("/files/{fileName}")
    public ResponseEntity<Resource> descargarFichero(@PathVariable String fileName) {

        // seguridad: evita ../
        Path p = downloadDir.resolve(fileName).normalize();
        if (!p.startsWith(downloadDir)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        FileSystemResource res = new FileSystemResource(p.toFile());
        if (!res.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(res);
    }
}

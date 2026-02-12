package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class PlcspXlsxImportService {

    // Executor para jobs (1 a la vez para evitar pisarse imports pesados)
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "plcsp-xlsx-import");
        t.setDaemon(true);
        return t;
    });

    // Estados de jobs
    private final ConcurrentMap<String, ImportJobStatus> jobs = new ConcurrentHashMap<>();

    /**
     * API actual: inicia importación async desde MultipartFile y devuelve jobId.
     */
    public String importarAsync(MultipartFile file) {
        Objects.requireNonNull(file, "file no puede ser null");

        String jobId = UUID.randomUUID().toString();
        ImportJobStatus status = new ImportJobStatus();
        status.setState(ImportJobStatus.State.RUNNING);
        jobs.put(jobId, status);

        executor.submit(() -> {
            try {
                doImport(file, status);
                status.setState(ImportJobStatus.State.DONE);
            } catch (Exception e) {
                status.setState(ImportJobStatus.State.ERROR);
            }
        });

        return jobId;
    }

    /**
     * ✅ NUEVO: inicia importación async desde una ruta local (Path) y devuelve jobId.
     * Reutiliza el mismo pipeline que MultipartFile.
     */
    public String importarAsync(Path xlsxPath) {
        Objects.requireNonNull(xlsxPath, "xlsxPath no puede ser null");
        MultipartFile mf = new PathMultipartFile("file", xlsxPath);
        return importarAsync(mf);
    }

    /**
     * Devuelve el estado del job.
     */
    public ImportJobStatus getStatus(String jobId) {
        return jobs.get(jobId);
    }

    /**
     * 🔧 AQUÍ VA TU LÓGICA REAL DE IMPORTACIÓN
     *
     * Este método debe:
     * - leer el XLSX (file.getInputStream() o file.getBytes())
     * - parsear filas
     * - hacer upsert en BD
     * - ir actualizando status: total, processed, inserted, updated, skipped, errors, etc.
     *
     * Como tú ya tienes esta lógica en tu proyecto, muévela aquí o llámala desde aquí.
     */
    private void doImport(MultipartFile file, ImportJobStatus status) throws Exception {

        // Ejemplo de “plantilla” (pon aquí tu código real):
        // -------------------------------------------------
        // byte[] bytes = file.getBytes();
        // List<Row> rows = parser.parse(bytes);
        // status.setTotal(rows.size());
        // for(Row r : rows){
        //   ... upsert ...
        //   status.incProcessed();
        // }
        //
        // -------------------------------------------------

        throw new UnsupportedOperationException(
                "Implementa aquí tu lógica real de importación (parse XLSX + upsert BD)."
        );
    }

    /**
     * MultipartFile respaldado por un Path (sin spring-test).
     * Permite reutilizar el flujo existente que espera MultipartFile.
     */
    private static final class PathMultipartFile implements MultipartFile {

        private final String name;
        private final Path path;

        private PathMultipartFile(String name, Path path) {
            this.name = Objects.requireNonNull(name);
            this.path = Objects.requireNonNull(path);
        }

        @Override public String getName() { return name; }

        @Override public String getOriginalFilename() {
            return path.getFileName() != null ? path.getFileName().toString() : "file.xlsx";
        }

        @Override public String getContentType() {
            // XLSX
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        @Override public boolean isEmpty() {
            try {
                return Files.size(path) == 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException e) {
                return 0L;
            }
        }

        @Override public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(path, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

package com.ordenatec.portallicitaciones.infra.importacion;

import com.ordenatec.portallicitaciones.application.service.ImportacionEuskadiService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler diario para importar Euskadi automáticamente.
 * (Opcional: si prefieres lanzarlo manual por endpoint, no la uses)
 */
@Component
public class ImportacionEuskadiScheduler {

    private final ImportacionEuskadiService service;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ImportacionEuskadiScheduler(ImportacionEuskadiService service) {
        this.service = service;
    }

    /**
     * Ejecuta cada día a las 06:10 (hora del servidor).
     * Ajusta el cron si lo quieres a otra hora.
     */
    @Scheduled(cron = "0 10 6 * * *")
    public void importarEuskadiDiario() {
        if (!running.compareAndSet(false, true)) return;

        try {
            // Hoy + últimos 7 días para captar cambios retroactivos
            service.importarDiario(7);
        } finally {
            running.set(false);
        }
    }
}

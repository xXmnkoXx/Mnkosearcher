package com.ordenatec.portallicitaciones.infra.importacion;

import com.ordenatec.portallicitaciones.application.service.ImportacionHaciendaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ImportacionScheduler {

    private final ImportacionHaciendaService service;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ImportacionScheduler(ImportacionHaciendaService service) {
        this.service = service;
    }

    // Cada 4 horas
    @Scheduled(cron = "0 0 */4 * * *")
    public void importarCada4Horas() {
        if (!running.compareAndSet(false, true)) return;
        try {
            service.importarUltimaPublicacion();
        } finally {
            running.set(false);
        }
    }
}

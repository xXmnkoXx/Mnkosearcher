package com.ordenatec.portallicitaciones.application.service;

import com.ordenatec.portallicitaciones.application.usecase.ImportarEuskadiDiarioUseCase;
import org.springframework.stereotype.Service;

@Service
public class ImportacionEuskadiService {

    private final ImportarEuskadiDiarioUseCase importarEuskadiDiarioUseCase;

    public ImportacionEuskadiService(ImportarEuskadiDiarioUseCase importarEuskadiDiarioUseCase) {
        this.importarEuskadiDiarioUseCase = importarEuskadiDiarioUseCase;
    }

    /**
     * Importación diaria Euskadi/Pais Vasco.
     * Se usa desde:
     *  - ImportacionesPaisVascoController (dedicado)
     */
    public ImportarEuskadiDiarioUseCase.Resultado importarDiario(int daysBack) {
        // Normaliza valores raros
        int safeDaysBack = Math.max(daysBack, 0);
        return importarEuskadiDiarioUseCase.ejecutar(safeDaysBack);
    }
}

package com.ordenatec.portallicitaciones.application.service;

import com.ordenatec.portallicitaciones.application.usecase.ImportarHaciendaUseCase;
import org.springframework.stereotype.Service;

@Service
public class ImportacionHaciendaService {

    private final ImportarHaciendaUseCase importarHaciendaUseCase;

    public ImportacionHaciendaService(ImportarHaciendaUseCase importarHaciendaUseCase) {
        this.importarHaciendaUseCase = importarHaciendaUseCase;
    }

    public ImportarHaciendaUseCase.Resultado importarUltimaPublicacion() {
        return importarHaciendaUseCase.ejecutar();
    }
}

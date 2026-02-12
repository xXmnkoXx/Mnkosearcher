package com.ordenatec.portallicitaciones.infra.persistence.repository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Proyección ligera para no cargar toda la entidad ni tener problemas de lazy loading en controller.
 */
public interface LicitacionLiteRow {
    String getExpediente();
    UUID getUuid();
    String getTitulo();
    String getTipoContratoTexto();
    LocalDate getFechaPublicacion();

    String getOrganismoNombre();
    String getOrganismoNif();
}

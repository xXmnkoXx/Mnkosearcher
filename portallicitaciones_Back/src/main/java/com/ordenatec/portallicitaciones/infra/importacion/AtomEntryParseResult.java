package com.ordenatec.portallicitaciones.infra.importacion;

import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.domain.model.LicitacionEvento;

public record AtomEntryParseResult(Licitacion licitacion, LicitacionEvento evento) {
}
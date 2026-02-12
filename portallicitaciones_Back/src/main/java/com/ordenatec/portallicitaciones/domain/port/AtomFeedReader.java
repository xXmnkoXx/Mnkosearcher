package com.ordenatec.portallicitaciones.domain.port;

import com.ordenatec.portallicitaciones.domain.model.Licitacion;

import java.io.InputStream;
import java.util.List;

public interface AtomFeedReader {

    List<Licitacion> leer(InputStream atomStream);
}

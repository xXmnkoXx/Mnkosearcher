package com.ordenatec.portallicitaciones.domain.port;

import java.io.InputStream;
import java.net.URI;

public interface ZipDownloader {
    InputStream descargar(URI zipUri);
}

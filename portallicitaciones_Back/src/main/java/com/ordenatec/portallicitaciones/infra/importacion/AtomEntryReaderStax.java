package com.ordenatec.portallicitaciones.infra.importacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utilidad para re-leer un único <entry> ATOM guardado en BD (raw_entry_xml),
 * envolviéndolo en un <feed> mínimo con los namespaces necesarios.
 *
 * NOTAS:
 * - En dumps de Hacienda muchos prefijos (cbc, cac, cac-place-ext, etc.) se declaran en <feed>.
 *   Si guardas solo <entry> sin esos xmlns, StAX puede fallar con "ElementPrefixUnbound".
 * - Si el raw_entry_xml está MAL FORMADO (tags sin cerrar), no es recuperable con StAX.
 */
@Component
public class AtomEntryReaderStax {

    private static final Logger log = LoggerFactory.getLogger(AtomEntryReaderStax.class);

    private final AtomFeedReaderStax feedReader;

    // Namespaces típicos del feed de Hacienda (si en tu feed real hay más, añádelos aquí).
    private static final String FEED_OPEN =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<feed xmlns=\"http://www.w3.org/2005/Atom\""
                    + " xmlns:cbc-place-ext=\"urn:dgpe:names:draft:codice-place-ext:schema:xsd:CommonBasicComponents-2\""
                    + " xmlns:cbc=\"urn:dgpe:names:draft:codice:schema:xsd:CommonBasicComponents-2\""
                    + " xmlns:cac=\"urn:dgpe:names:draft:codice:schema:xsd:CommonAggregateComponents-2\""
                    + " xmlns:cac-place-ext=\"urn:dgpe:names:draft:codice-place-ext:schema:xsd:CommonAggregateComponents-2\""
                    + " xmlns:at=\"http://purl.org/atompub/tombstones/1.0\""
                    + " xmlns:ns7=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\""
                    + ">";

    private static final String FEED_CLOSE = "</feed>";

    public AtomEntryReaderStax(AtomFeedReaderStax feedReader) {
        this.feedReader = feedReader;
    }

    /**
     * Parsea un único <entry>...</entry> y devuelve su resultado.
     *
     * @param rawEntryXml XML que contiene el entry completo (tal cual guardado en BD)
     * @return AtomEntryParseResult (Licitacion + Evento) o null si no se pudo parsear
     */
    public AtomEntryParseResult parseSingleEntry(String rawEntryXml) {
        if (rawEntryXml == null || rawEntryXml.isBlank()) return null;

        // 0) Normaliza: quita prólogo XML y extrae SOLO el <entry>...</entry> si viene “sucio”
        String entry = normalizeToEntry(rawEntryXml);
        if (entry == null || entry.isBlank()) return null;

        // 1) Asegura que el <entry> está en el namespace de ATOM si viene "pelado"
        //    (muchos dumps traen <entry> sin xmlns porque hereda de <feed>).
        entry = ensureAtomNamespaceOnEntry(entry);

        // 2) Envuelve en un feed con los xmlns necesarios (cbc/cac/...).
        String wrapped = FEED_OPEN + entry + FEED_CLOSE;

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(wrapped.getBytes(StandardCharsets.UTF_8));
            List<AtomEntryParseResult> results = feedReader.leerConEventos(in, null, null);
            if (results == null || results.isEmpty()) return null;
            return results.get(0);
        } catch (Exception ex) {
            // IMPORTANTE: No re-lanzar aquí, para que el enriquecimiento pueda continuar con el siguiente evento.
            // Si el entry está MAL FORMADO, aquí fallará siempre (y está bien: devolvemos null).
            log.warn("Error parseando entry envuelto en feed (se omite): {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Normaliza el string para quedarnos con un fragmento <entry>...</entry> sin prólogo XML.
     * - Quita '<?xml ...?>' si existe
     * - Si hay texto antes/después, recorta desde el primer '<entry' hasta el último '</entry>'
     */
    private static String normalizeToEntry(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // Quita prólogo XML si existe
        s = s.replaceFirst("^\\s*<\\?xml[^>]*\\?>\\s*", "");

        // Si ya empieza por <entry, ok
        if (s.startsWith("<entry")) return s;

        // Si viene con basura alrededor, intenta extraer el bloque <entry>...</entry>
        int start = s.indexOf("<entry");
        if (start < 0) return s; // no parece entry, lo dejamos tal cual (fallará y devolverá null)

        int end = s.lastIndexOf("</entry>");
        if (end < 0) return s.substring(start); // no hay cierre, será mal formado igualmente

        end = end + "</entry>".length();
        return s.substring(start, end).trim();
    }

    /**
     * Si el entry empieza por "<entry>" o "<entry ...>" pero NO contiene xmlns="http://www.w3.org/2005/Atom",
     * lo inserta en la etiqueta de apertura.
     */
    private static String ensureAtomNamespaceOnEntry(String entry) {
        if (entry == null) return null;

        String s = entry.trim();

        // Si no parece un entry, lo dejamos tal cual (lo intentará parsear y devolverá null si falla).
        if (!s.startsWith("<entry")) return s;

        // Si ya trae xmlns Atom, no tocar.
        if (s.contains("xmlns=\"http://www.w3.org/2005/Atom\"")) return s;

        // Inserta xmlns en la primera etiqueta de apertura <entry ...>
        int endOfOpenTag = s.indexOf('>');
        if (endOfOpenTag < 0) return s;

        String openTag = s.substring(0, endOfOpenTag);
        String rest = s.substring(endOfOpenTag);

        // Si ya tiene algún xmlns, añadimos igualmente el default Atom.
        if (openTag.contains("xmlns=")) {
            return openTag + " xmlns=\"http://www.w3.org/2005/Atom\"" + rest;
        }

        return openTag + " xmlns=\"http://www.w3.org/2005/Atom\"" + rest;
    }
}

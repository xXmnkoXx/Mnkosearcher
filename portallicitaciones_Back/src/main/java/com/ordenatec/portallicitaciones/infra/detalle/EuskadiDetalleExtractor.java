package com.ordenatec.portallicitaciones.infra.detalle;

import com.ordenatec.portallicitaciones.infra.http.DetalleHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae datos del HTML público de Euskadi (contratacion.euskadi.eus).
 * - Origen: url_publica (index.html)
 * - Salida: DetalleExtraido (organo, entidad, procedimiento, estado, lugar, nuts, cpvs, importes...)
 */
@Component
public class EuskadiDetalleExtractor implements DetalleExtractor {

    private static final Pattern CPV_8 = Pattern.compile("\\b\\d{8}\\b");
    private static final Pattern MONEY_EUR = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)\\s*€");
    private static final Pattern NUTS = Pattern.compile("\\bES\\d{3}(?:[A-Z0-9]{2})?\\b");

    @Override
    public DetalleExtraido extraer(DetalleHttpClient.FetchResult fetch) {
        if (fetch == null || fetch.body() == null || fetch.body().isBlank()) {
            return DetalleExtraido.vacio();
        }

        String html = fetch.body();
        Document doc = Jsoup.parse(html);

        // Campos típicos (tabla etiqueta/valor)
        String organo = findValueByLabel(doc,
                "Órgano de contratación", "Organo de contratacion", "Órgano contratación", "Órgano");
        String entidad = findValueByLabel(doc,
                "Entidad", "Entidad adjudicadora", "Poder adjudicador");
        String procedimiento = findValueByLabel(doc,
                "Procedimiento", "Tipo de procedimiento");
        String modalidad = findValueByLabel(doc,
                "Tramitación", "Tramitacion", "Modalidad");
        String estado = findValueByLabel(doc,
                "Estado", "Situación", "Situacion");
        String lugar = findValueByLabel(doc,
                "Lugar de ejecución", "Lugar de ejecucion", "Lugar");

        String nuts = extractFirstNuts(doc.text());

        // Importes (si aparecen en tabla / texto)
        BigDecimal presupuesto = parseEuro(findValueByLabel(doc,
                "Presupuesto base de licitación", "Presupuesto base de licitacion", "Presupuesto"));
        BigDecimal valorEstimado = parseEuro(findValueByLabel(doc,
                "Valor estimado", "Valor Estimado"));

        // CPVs (suelen aparecer en el HTML como códigos)
        Set<String> cpvs = extractCpvs(doc.text());

        return DetalleExtraido.builder()
                // meta fetch
                .urlFinal(fetch.finalUrl())
                .contentType(fetch.contentType())

                // básicos
                .organo(organo)
                .entidad(entidad)
                .procedimiento(procedimiento)
                .modalidad(modalidad)
                .estadoTexto(estado)
                .lugarEjecucion(lugar)
                .codigoNuts(nuts)

                // extensiones
                .cpvCodes(cpvs)
                .presupuesto(presupuesto)
                .valorEstimado(valorEstimado)
                .moneda((presupuesto != null || valorEstimado != null) ? "EUR" : null)
                .build();
    }

    private static Set<String> extractCpvs(String text) {
        if (text == null) return Set.of();
        Matcher m = CPV_8.matcher(text);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        while (m.find()) {
            out.add(m.group());
            if (out.size() >= 30) break; // defensivo
        }
        return out;
    }

    private static BigDecimal parseEuro(String s) {
        if (s == null) return null;
        Matcher m = MONEY_EUR.matcher(s);
        if (!m.find()) return null;

        String raw = m.group(1);                 // ej: "12.345,67"
        String normalized = raw.replace(".", "").replace(",", ".");
        try {
            return new BigDecimal(normalized);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String extractFirstNuts(String text) {
        if (text == null) return null;
        Matcher m = NUTS.matcher(text);
        return m.find() ? m.group() : null;
    }

    /**
     * Busca valor por etiqueta en tablas/listados tipo "Etiqueta -> Valor".
     * 1) tr th/td
     * 2) fallback: intenta encontrar el texto de la etiqueta en el documento.
     */
    private static String findValueByLabel(Document doc, String... labelCandidates) {
        if (doc == null || labelCandidates == null) return null;

        List<String> labels = Arrays.stream(labelCandidates)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Caso 1: filas de tabla
        Elements rows = doc.select("tr");
        for (Element tr : rows) {
            Elements cells = tr.select("th,td");
            if (cells.size() < 2) continue;

            String left = clean(cells.get(0).text());
            String right = clean(cells.get(1).text());
            if (left == null || right == null) continue;

            for (String lab : labels) {
                if (containsIgnoreCase(left, lab)) {
                    return right;
                }
            }
        }

        // Caso 2: fallback - etiqueta en texto (devuelve snippet corto)
        String text = doc.text();
        if (text == null) return null;

        String lower = text.toLowerCase(Locale.ROOT);
        for (String lab : labels) {
            String ll = lab.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf(ll);
            if (idx >= 0) {
                int end = Math.min(text.length(), idx + 220);
                String snippet = text.substring(idx, end);
                return clean(snippet);
            }
        }

        return null;
    }

    private static boolean containsIgnoreCase(String hay, String needle) {
        if (hay == null || needle == null) return false;
        return hay.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ').trim();
        t = t.replaceAll("\\s+", " ");
        return t.isBlank() ? null : t;
    }
}

package com.ordenatec.portallicitaciones.infra.detalle;

import com.ordenatec.portallicitaciones.infra.http.DetalleHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class EstadoDetalleExtractor implements DetalleExtractor {

    @Override
    public DetalleExtraido extraer(DetalleHttpClient.FetchResult fetch) {
        if (fetch == null || fetch.body() == null || fetch.body().isBlank()) {
            return DetalleExtraido.vacio();
        }

        String html = fetch.body();
        Document doc = Jsoup.parse(html);

        // 1) Intento: tablas (th/td o td/td)
        String estado = buscarEnTablas(doc);
        if (estado != null) {
            return DetalleExtraido.builder().estadoTexto(estado).build();
        }

        // 2) Intento: definición (dl dt/dd)
        estado = buscarEnDl(doc);
        if (estado != null) {
            return DetalleExtraido.builder().estadoTexto(estado).build();
        }

        // 3) Intento: pares label/valor en divs/spans
        estado = buscarEnParesLabelValor(doc);
        if (estado != null) {
            return DetalleExtraido.builder().estadoTexto(estado).build();
        }

        // 4) Fallback: texto plano
        estado = buscarPorTextoPlano(doc);
        if (estado != null) {
            return DetalleExtraido.builder().estadoTexto(estado).build();
        }

        return DetalleExtraido.vacio();
    }

    private static String buscarEnTablas(Document doc) {
        Elements rows = doc.select("table tr");
        for (Element tr : rows) {
            Elements ths = tr.select("th");
            Elements tds = tr.select("td");

            // Caso A: th(label) + td(valor)
            if (!ths.isEmpty() && !tds.isEmpty()) {
                String label = norm(ths.first().text());
                if (esLabelEstado(label)) {
                    String val = cleanValue(tds.first().text());
                    if (val != null) return val;
                }
            }

            // Caso B: td(label) + td(valor)
            if (tds.size() >= 2) {
                String label = norm(tds.get(0).text());
                if (esLabelEstado(label)) {
                    String val = cleanValue(tds.get(1).text());
                    if (val != null) return val;
                }
            }
        }
        return null;
    }

    private static String buscarEnDl(Document doc) {
        Elements dts = doc.select("dl dt");
        for (Element dt : dts) {
            String label = norm(dt.text());
            if (!esLabelEstado(label)) continue;

            Element dd = dt.nextElementSibling();
            if (dd != null && dd.tagName().equalsIgnoreCase("dd")) {
                String val = cleanValue(dd.text());
                if (val != null) return val;
            }
        }
        return null;
    }

    private static String buscarEnParesLabelValor(Document doc) {
        Elements labelCandidates = doc.select("span, strong, b, label, p, div, th, dt");
        for (Element el : labelCandidates) {
            String label = norm(el.text());
            if (!esLabelEstado(label)) continue;

            // Intento 1: hermano siguiente
            Element next = el.nextElementSibling();
            if (next != null) {
                String val = cleanValue(next.text());
                if (val != null) return val;
            }

            // Intento 2: mismo contenedor
            Element parent = el.parent();
            if (parent != null) {
                Elements vals = parent.select("span, div, p, td, dd");
                for (Element v : vals) {
                    if (v == el) continue;
                    String val = cleanValue(v.text());
                    if (val != null && !esLabelEstado(norm(val))) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    private static String buscarPorTextoPlano(Document doc) {
        String text = doc.text();
        if (text == null || text.isBlank()) return null;

        String lower = norm(text);

        // Claves típicas (Euskadi usa "Estado de la tramitación")
        String[] keys = new String[] {
                "estado de la tramitacion",
                "estado del expediente",
                "estado",
                "situacion",
                "fase",
                "fase del expediente"
        };

        for (String key : keys) {
            int idx = lower.indexOf(key);
            if (idx < 0) continue;

            int start = idx + key.length();
            int end = Math.min(text.length(), start + 160);
            String window = text.substring(start, end);

            int colon = window.indexOf(':');
            if (colon >= 0) {
                String after = window.substring(colon + 1);
                String val = cleanValue(recortarHastaCorte(after));
                if (val != null) return val;
            } else {
                String val = cleanValue(recortarHastaCorte(window));
                if (val != null) return val;
            }
        }

        return null;
    }

    private static String recortarHastaCorte(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ').trim();

        String[] cuts = new String[] {"  ", " | ", " - ", " – ", " · ", ".", ";", ","};
        int best = -1;
        for (String c : cuts) {
            int i = t.indexOf(c);
            if (i >= 0 && (best < 0 || i < best)) best = i;
        }
        if (best >= 0) t = t.substring(0, best);

        return t.trim();
    }

    private static boolean esLabelEstado(String normalizedLabel) {
        if (normalizedLabel == null || normalizedLabel.isBlank()) return false;

        String l = normalizedLabel;

        // Variantes comunes (incluye Euskadi)
        return l.equals("estado")
                || l.equals("estado del expediente")
                || l.equals("estado de la tramitacion")
                || l.equals("estado de tramitacion")
                || l.equals("situacion")
                || l.equals("fase")
                || l.equals("fase del expediente")
                || l.equals("tramitacion");
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.replace('\u00A0', ' ').trim().toLowerCase(Locale.ROOT);

        // quitamos ":" final frecuente
        while (t.endsWith(":")) t = t.substring(0, t.length() - 1).trim();

        // normaliza y elimina tildes para que "tramitación" == "tramitacion"
        t = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // compacta espacios
        t = t.replaceAll("\\s+", " ").trim();

        return t;
    }

    private static String cleanValue(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ').trim();
        t = t.replaceAll("\\s+", " ").trim();

        if (t.isEmpty()) return null;

        // Evita devolver el propio label como valor
        String n = norm(t);
        if (esLabelEstado(n)) return null;

        // quita prefijos raros
        t = t.replaceAll("^[\\p{Punct}\\s]+", "").trim();

        return t.isEmpty() ? null : t;
    }
}

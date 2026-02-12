package com.ordenatec.portallicitaciones.application.usecase;

import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEventoEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionEventoJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Repara histórico corrupto por el bug antiguo:
 *  - START: <cbc:ContractFolderID>
 *  - END  : </ContractFolderID>  (sin prefijo)  -> MAL FORMADO
 *
 * Este use case reconstruye los cierres con el prefijo correcto usando una pila.
 */
@Component
public class RepararRawEntryXmlHistoricoUseCase {

    private static final Logger log = LoggerFactory.getLogger(RepararRawEntryXmlHistoricoUseCase.class);

    public record Resultado(
            int eventosTotal,
            int eventosConRaw,
            int eventosDetectadosCorruptos,
            int eventosReparados,
            int eventosSinCambios
    ) {}

    private final LicitacionEventoJpaRepository eventoRepo;

    public RepararRawEntryXmlHistoricoUseCase(LicitacionEventoJpaRepository eventoRepo) {
        this.eventoRepo = eventoRepo;
    }

    /**
     * Repara TODOS los eventos (pensado para ejecución puntual).
     * Si quieres, luego lo limitamos por daysBack o por source_zip.
     */
    @Transactional
    public Resultado ejecutar() {
        List<LicitacionEventoEntity> all = eventoRepo.findAll();

        int total = all.size();
        int conRaw = 0;
        int corruptos = 0;
        int reparados = 0;
        int sinCambios = 0;

        for (LicitacionEventoEntity ev : all) {
            String raw = ev.getRawEntryXml();
            if (raw == null || raw.isBlank()) continue;
            conRaw++;

            // Heurística barata: si hay prefijos ":" pero también hay cierres sin ":" -> probable corrupción
            if (!seVeCorruptoPorPrefijo(raw)) {
                sinCambios++;
                continue;
            }
            corruptos++;

            String fixed = repairMissingPrefixEndTags(raw);

            if (fixed != null && !fixed.equals(raw)) {
                ev.setRawEntryXml(fixed);
                // save no es estrictamente necesario dentro de @Transactional si la entidad está gestionada,
                // pero lo dejamos explícito por claridad.
                eventoRepo.save(ev);
                reparados++;
            } else {
                sinCambios++;
            }
        }

        log.info("[REPARAR-RAW] total={} conRaw={} corruptos={} reparados={} sinCambios={}",
                total, conRaw, corruptos, reparados, sinCambios);

        return new Resultado(total, conRaw, corruptos, reparados, sinCambios);
    }

    /**
     * Detecta patrón típico:
     *  - hay tags con prefijo (ej. "<cbc:")  -> contiene ":"
     *  - pero hay cierres sin prefijo del tipo "</ContractFolderID>" -> "</[A-Za-z0-9_]+>"
     *
     * No es perfecto, pero suficiente para decidir “intentar reparar”.
     */
    private static boolean seVeCorruptoPorPrefijo(String xml) {
        String s = xml;
        if (!s.contains(":")) return false;

        // cierres sin ":" (muy típico del bug). Si ya hay muchos cierres con ":" no pasa nada, pero con que exista
        // alguno sin ":" ya nos interesa intentar reparar.
        // Nota: lo hacemos simple para no meter regex pesada.
        int idx = 0;
        while ((idx = s.indexOf("</", idx)) >= 0) {
            int gt = s.indexOf('>', idx + 2);
            if (gt < 0) break;
            String name = s.substring(idx + 2, gt).trim();
            // cierre sin prefijo y “simple”
            if (!name.contains(":") && name.matches("[A-Za-z0-9_\\-\\.]+")) return true;
            idx = gt + 1;
        }
        return false;
    }

    /**
     * Reparación principal: escanea tokens "<...>" y usa pila de tags abiertos.
     * Si encuentra un cierre </Local> y el último abierto fue <prefix:Local>, lo corrige a </prefix:Local>.
     *
     * *No* intenta arreglar otros tipos de XML malformado.
     */
    static String repairMissingPrefixEndTags(String xml) {
        if (xml == null || xml.isBlank()) return xml;

        String s = xml;
        StringBuilder out = new StringBuilder(s.length() + 1024);
        Deque<QName> stack = new ArrayDeque<>();

        int i = 0;
        while (i < s.length()) {
            int lt = s.indexOf('<', i);
            if (lt < 0) {
                out.append(s, i, s.length());
                break;
            }
            // texto
            out.append(s, i, lt);

            int gt = s.indexOf('>', lt + 1);
            if (gt < 0) {
                // no hay cierre de tag, devolvemos tal cual
                out.append(s.substring(lt));
                break;
            }

            String token = s.substring(lt, gt + 1);

            // comentarios / CDATA / PI -> copiar tal cual
            if (token.startsWith("<!--") || token.startsWith("<![CDATA[") || token.startsWith("<?")) {
                out.append(token);
                i = gt + 1;
                continue;
            }

            // cierre
            if (token.startsWith("</")) {
                String name = token.substring(2, token.length() - 1).trim();
                String fixedName = name;

                if (!name.contains(":")) {
                    // si el top de la pila tiene mismo local y tiene prefijo -> corregimos
                    QName top = stack.peekLast();
                    if (top != null && top.local.equals(name) && top.prefix != null && !top.prefix.isBlank()) {
                        fixedName = top.prefix + ":" + top.local;
                    } else {
                        // tolerante: busca hacia abajo por si hay desajuste puntual
                        QName found = findFromTopByLocal(stack, name);
                        if (found != null && found.prefix != null && !found.prefix.isBlank()) {
                            fixedName = found.prefix + ":" + found.local;
                            // y “re-sincroniza” la pila: desapila hasta ese found
                            popUntilLocal(stack, name);
                        }
                    }
                } else {
                    // si viene con prefijo, sincroniza pila si cuadra con el top
                    QName top = stack.peekLast();
                    if (top != null && (top.prefix + ":" + top.local).equals(name)) {
                        stack.pollLast();
                    } else {
                        // intenta desapilar hasta que lo encuentre (tolerante)
                        popUntilQName(stack, name);
                    }
                }

                // si hemos corregido usando el top normal, desapilamos 1 (caso habitual)
                if (!stack.isEmpty()) {
                    QName top = stack.peekLast();
                    String topQName = (top.prefix == null || top.prefix.isBlank()) ? top.local : top.prefix + ":" + top.local;
                    if (fixedName.equals(topQName)) {
                        stack.pollLast();
                    }
                }

                out.append("</").append(fixedName).append(">");
                i = gt + 1;
                continue;
            }

            // apertura o self-closing
            boolean selfClosing = token.endsWith("/>");
            String inside = token.substring(1, token.length() - (selfClosing ? 2 : 1)).trim();

            // nombre del tag: hasta espacio o fin
            String tagName = readTagName(inside);
            if (tagName.isBlank()) {
                out.append(token);
                i = gt + 1;
                continue;
            }

            QName qn = parseQName(tagName);
            if (!selfClosing) stack.addLast(qn);

            out.append(token);
            i = gt + 1;
        }

        return out.toString();
    }

    private static String readTagName(String inside) {
        int sp = inside.indexOf(' ');
        if (sp < 0) return inside;
        return inside.substring(0, sp);
    }

    private static QName parseQName(String tagName) {
        int c = tagName.indexOf(':');
        if (c < 0) return new QName(null, tagName);
        return new QName(tagName.substring(0, c), tagName.substring(c + 1));
    }

    private static QName findFromTopByLocal(Deque<QName> stack, String local) {
    if (stack.isEmpty()) return null;

    Iterator<QName> it = stack.descendingIterator(); // desde tail -> head
    while (it.hasNext()) {
        QName q = it.next();
        if (q.local.equals(local)) return q;
    }
    return null;
}

    private static void popUntilLocal(Deque<QName> stack, String local) {
        while (!stack.isEmpty()) {
            QName q = stack.peekLast();
            if (q.local.equals(local)) return;
            stack.pollLast();
        }
    }

    private static void popUntilQName(Deque<QName> stack, String qname) {
        while (!stack.isEmpty()) {
            QName q = stack.peekLast();
            String topQName = (q.prefix == null || q.prefix.isBlank()) ? q.local : q.prefix + ":" + q.local;
            if (topQName.equals(qname)) return;
            stack.pollLast();
        }
    }

    private record QName(String prefix, String local) {}
}

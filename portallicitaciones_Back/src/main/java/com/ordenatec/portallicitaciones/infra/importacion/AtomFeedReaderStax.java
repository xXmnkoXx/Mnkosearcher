package com.ordenatec.portallicitaciones.infra.importacion;

import com.ordenatec.portallicitaciones.domain.enums.EstadoLicitacion;
import com.ordenatec.portallicitaciones.domain.enums.TipoContrato;
import com.ordenatec.portallicitaciones.domain.model.*;
import org.springframework.stereotype.Component;

import javax.xml.stream.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Lee ficheros ATOM de contratación (Hacienda / PLACSP) y construye:
 *  - Licitacion (foto actual normalizada)
 *  - LicitacionEvento (histórico: rawEntryXml + atomEntryId/updated/published + trazabilidad)
 */
@Component
public class AtomFeedReaderStax {

    private final XMLInputFactory factory;

    /**
     * '&' sin escapar es el fallo más típico en ATOMs publicados (descripciones con "I+D & Innovación", etc.)
     * Este patrón reemplaza solo los '&' que NO inician una entidad XML válida.
     */
    private static final Pattern UNESCAPED_AMP = Pattern.compile(
            "&(?!#\\d+;|#x[0-9A-Fa-f]+;|[A-Za-z][A-Za-z0-9]+;)"
    );

    public AtomFeedReaderStax() {
        this.factory = XMLInputFactory.newFactory();
        // Seguridad básica contra XXE
        try { factory.setProperty(XMLInputFactory.SUPPORT_DTD, false); } catch (Exception ignore) {}
        try { factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false); } catch (Exception ignore) {}
    }

    /** Legacy: devuelve solo licitaciones (sin eventos). */
    public List<Licitacion> leer(InputStream atomStream) {
        List<AtomEntryParseResult> res = leerConEventos(atomStream, null, null);
        List<Licitacion> out = new ArrayList<>(res.size());
        for (AtomEntryParseResult r : res) out.add(r.licitacion());
        return out;
    }

    /** Recomendado: devuelve licitación + evento por cada <entry>. */
    public List<AtomEntryParseResult> leerConEventos(InputStream atomStream,
                                                     String sourceZip,
                                                     String sourceAtomFile) {
        try {
            // IMPORTANTÍSIMO:
            // Algunos ATOM vienen con XML no bien formado (caracteres de control o '&' sin escapar).
            // StAX es estricto y aborta toda la importación con XMLStreamException.
            // Sanitizamos el XML antes de parsearlo.
            String xml = sanitizeXml(readAllToString(atomStream));
            XMLStreamReader r = factory.createXMLStreamReader(new StringReader(xml));

            List<AtomEntryParseResult> out = new ArrayList<>();
            Deque<String> stack = new ArrayDeque<>();
            StringBuilder text = new StringBuilder();

            boolean inEntry = false;
            StringWriter entryXml = null;

            Licitacion lic = null;
            LicitacionEvento ev = null;

            // Contextos
            Documento doc = null;
            Lote lote = null;
            OrganismoContratacion org = null;
            Adjudicacion adjud = null;

            // Para capturar currencyID de importes
            String currentCurrencyId = null;

            // Para capturar atributos que luego necesitas en END_ELEMENT
            String currentIdSchemeName = null;     // schemeName del último <ID ...>
            String currentDurationUnitCode = null; // unitCode del último <DurationMeasure ...>

            // Helpers para “dónde estoy” en el UBL
            boolean inAdditionalDocumentReference = false;
            boolean inLot = false;

            // Para adjudicación: winner party data
            boolean inWinnerParty = false;

            while (r.hasNext()) {
                int event = r.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = r.getLocalName();
                    stack.addLast(local);
                    text.setLength(0);

                    if ("entry".equalsIgnoreCase(local)) {
                        inEntry = true;
                        entryXml = new StringWriter(24_000);

                        lic = new Licitacion();
                        lic.setId(UUID.randomUUID());

                        ev = new LicitacionEvento();
                        ev.setId(UUID.randomUUID());
                        ev.setSourceZip(sourceZip);
                        ev.setSourceAtomFile(sourceAtomFile);

                        if (lic.getFechas() == null) lic.setFechas(new FechasProcedimiento());
                    }

                    if (inEntry && entryXml != null) {
                        writeStart(r, entryXml);
                    }

                    // Capturar currencyID si el elemento lo trae
                    currentCurrencyId = attr(r, "currencyID");

                    // Capturar schemeName cuando abrimos un <ID ...schemeName="...">
                    if ("ID".equalsIgnoreCase(local)) {
                        currentIdSchemeName = attr(r, "schemeName");
                    }

                    // Capturar unitCode cuando abrimos un <DurationMeasure unitCode="...">
                    if ("DurationMeasure".equalsIgnoreCase(local)) {
                        currentDurationUnitCode = attr(r, "unitCode");
                    }

                    // link href (ATOM)
                    if (inEntry && "link".equalsIgnoreCase(local) && lic != null && ev != null) {
                        String href = attr(r, "href");
                        if (href != null && !href.isBlank()) {
                            lic.setUrlPublica(href.trim());
                            ev.setUrlPublica(href.trim());
                        }
                    }

                    // Documento
                    if (inEntry && "AdditionalDocumentReference".equalsIgnoreCase(local)) {
                        inAdditionalDocumentReference = true;
                        doc = new Documento();
                    }

                    // Lote
                    if (inEntry && ("ProcurementProjectLot".equalsIgnoreCase(local) || "Lot".equalsIgnoreCase(local))) {
                        inLot = true;
                        lote = new Lote();
                    }

                    // Organismo
                    if (inEntry && "Party".equalsIgnoreCase(local) && org == null) {
                        org = new OrganismoContratacion();
                    }

                    // Adjudicación
                    if (inEntry && equalsAny(local, "TenderResult", "AwardedTenderedProject", "ResultOfTheAward")) {
                        if (adjud == null) adjud = new Adjudicacion();
                    }

                    // Winner party
                    if (inEntry && adjud != null && equalsAny(local, "WinningParty", "AwardingParty", "Party")) {
                        if (contains(stack, "TenderResult") || contains(stack, "AwardedTenderedProject") || contains(stack, "ResultOfTheAward")) {
                            inWinnerParty = true;
                        }
                    }

                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    String t = r.getText();
                    text.append(t);
                    if (inEntry && entryXml != null) entryXml.write(escapeText(t));

                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = r.getLocalName();
                    String value = text.toString().trim();

                    if (inEntry && entryXml != null) {
                        // IMPORTANTE: cerrar con el QName completo (incluye prefijo) para no corromper rawEntryXml
                        writeEnd(r, entryXml);
                    }

                    if (inEntry && lic != null && ev != null) {

                        // -------------------------
                        // ATOM base
                        // -------------------------
                        if (isPath(stack, "entry", "id") && "id".equalsIgnoreCase(local) && !value.isBlank()) {
                            ev.setAtomEntryId(value);
                            lic.setLastEventEntryId(value);
                        }

                        if (isPath(stack, "entry", "updated") && "updated".equalsIgnoreCase(local) && !value.isBlank()) {
                            Instant upd = parseInstant(value);
                            lic.setLastEventUpdatedAt(upd);
                            ev.setAtomUpdatedAt(upd);
                        }

                        if (isPath(stack, "entry", "published") && "published".equalsIgnoreCase(local) && !value.isBlank()) {
                            ev.setAtomPublishedAt(parseInstant(value));
                        }

                        if (isPath(stack, "entry", "title") && "title".equalsIgnoreCase(local) && !value.isBlank()) {
                            lic.setTitulo(value);
                        }

                        if (isPath(stack, "entry", "summary") && "summary".equalsIgnoreCase(local) && !value.isBlank()) {
                            ev.setChangeSummary(value);
                        }

                        // -------------------------
                        // UBL claves de licitación
                        // -------------------------
                        if ("ContractFolderID".equalsIgnoreCase(local) && !value.isBlank()) {
                            lic.setExpediente(value);
                            ev.setExpediente(value);
                        }

                        if ("ID".equalsIgnoreCase(local) && contains(stack, "ContractFolderStatus") && !value.isBlank()) {
                            if (lic.getReferenciaPlataforma() == null) lic.setReferenciaPlataforma(value);
                        }

                        if ("ContractFolderStatusCode".equalsIgnoreCase(local) && !value.isBlank()) {
                            try { lic.setEstado(EstadoLicitacion.valueOf(value)); } catch (Exception ignore) {}
                        }

                        if ("TypeCode".equalsIgnoreCase(local) && !value.isBlank()
                                && (contains(stack, "ProcurementProject") || contains(stack, "ProcurementProjectLot"))) {
                            lic.setTipoContrato(mapTipoContrato(value));
                        }

                        if ("ProcedureCode".equalsIgnoreCase(local) && !value.isBlank()) {
                            lic.setProcedimiento(value);
                        }

                        if ("UrgencyCode".equalsIgnoreCase(local) && !value.isBlank()) {
                            lic.setTramitacion(value);
                        }

                        if ("SRA".equalsIgnoreCase(local) && !value.isBlank()) {
                            lic.setSra(parseBooleanLoose(value));
                        }

                        // -------------------------
                        // FECHAS
                        // -------------------------
                        FechasProcedimiento fechas = lic.getFechas();
                        if (fechas == null) {
                            fechas = new FechasProcedimiento();
                            lic.setFechas(fechas);
                        }

                        if ("IssueDate".equalsIgnoreCase(local) && !value.isBlank()) {
                            fechas.setFechaPublicacion(parseLocalDate(value));
                        }

                        if ("EndDate".equalsIgnoreCase(local) && !value.isBlank() && contains(stack, "TenderSubmissionDeadlinePeriod")) {
                            fechas.setFechaLimitePresentacion(parseLocalDate(value));
                        }

                        if ("StartDate".equalsIgnoreCase(local) && !value.isBlank() && contains(stack, "TenderOpening")) {
                            fechas.setFechaApertura(parseLocalDate(value));
                        }
                        if ("EndDate".equalsIgnoreCase(local) && !value.isBlank() && contains(stack, "TenderOpening")) {
                            fechas.setFechaApertura(parseLocalDate(value));
                        }

                        if ("AwardDate".equalsIgnoreCase(local) && !value.isBlank()) {
                            fechas.setFechaAdjudicacion(parseLocalDate(value));
                        }
                        if ("ContractSignatureDate".equalsIgnoreCase(local) && !value.isBlank()) {
                            fechas.setFechaFormalizacion(parseLocalDate(value));
                        }

                        // -------------------------
                        // DINERO
                        // -------------------------
                        if ("EstimatedOverallContractAmount".equalsIgnoreCase(local) && !value.isBlank()) {
                            lic.setValorEstimado(toMoney(value, currentCurrencyId, null));
                            if (lic.getMoneda() == null && currentCurrencyId != null) lic.setMoneda(currentCurrencyId);
                        }

                        if ("TaxExclusiveAmount".equalsIgnoreCase(local) && !value.isBlank()) {
                            lic.setPresupuestoBase(toMoney(value, currentCurrencyId, false));
                            if (lic.getMoneda() == null && currentCurrencyId != null) lic.setMoneda(currentCurrencyId);
                        }

                        if ("TotalAmount".equalsIgnoreCase(local) && !value.isBlank()
                                && (contains(stack, "LegalMonetaryTotal") || contains(stack, "RequestedTenderTotal") || contains(stack, "AnticipatedMonetaryTotal"))) {
                            lic.setPresupuestoBase(toMoney(value, currentCurrencyId, true));
                            if (lic.getMoneda() == null && currentCurrencyId != null) lic.setMoneda(currentCurrencyId);
                        }

                        // -------------------------
                        // CPV (licitación)
                        // -------------------------
                        if ("ItemClassificationCode".equalsIgnoreCase(local) && !value.isBlank() && !inLot) {
                            Cpv cpv = new Cpv();
                            cpv.setCodigo(value);
                            if (lic.getCpvs().isEmpty()) cpv.setPrincipal(true);
                            lic.getCpvs().add(cpv);
                        }

                        // -------------------------
                        // ORGANISMO
                        // -------------------------
                        if (org != null) {
                            if ("Name".equalsIgnoreCase(local) && contains(stack, "PartyName") && !value.isBlank()) {
                                if (isBlank(org.getNombre())) org.setNombre(value);
                            }
                            if ("ID".equalsIgnoreCase(local) && contains(stack, "PartyIdentification") && !value.isBlank()) {
                                String scheme = currentIdSchemeName;
                                if ("NIF".equalsIgnoreCase(scheme)) org.setNif(value);
                                if ("DIR3".equalsIgnoreCase(scheme)) org.setDir3(value);
                            }
                            if ("Party".equalsIgnoreCase(local) && !inWinnerParty) {
                                if (notBlank(org.getNombre()) || notBlank(org.getNif()) || notBlank(org.getDir3())) {
                                    lic.setOrganismo(org);
                                }
                            }
                        }

                        // -------------------------
                        // DOCUMENTOS
                        // -------------------------
                        if (inAdditionalDocumentReference && doc != null) {
                            if ("DocumentTypeCode".equalsIgnoreCase(local) && !value.isBlank()) doc.setTipo(value);
                            if ("FileName".equalsIgnoreCase(local) && !value.isBlank()) doc.setTitulo(value);
                            if ("URI".equalsIgnoreCase(local) && !value.isBlank()) doc.setUrl(value);

                            if ("IssueDate".equalsIgnoreCase(local) && !value.isBlank() && contains(stack, "AdditionalDocumentReference")) {
                                Instant fi = parseInstantFromDate(value);
                                if (fi != null) doc.setFecha(fi);
                            }

                            if ("AdditionalDocumentReference".equalsIgnoreCase(local)) {
                                if (notBlank(doc.getUrl())) lic.getDocumentos().add(doc);
                                doc = null;
                                inAdditionalDocumentReference = false;
                            }
                        }

                        // -------------------------
                        // LOTES
                        // -------------------------
                        if (inLot && lote != null) {
                            if (("ID".equalsIgnoreCase(local) || "LotID".equalsIgnoreCase(local)) && !value.isBlank()
                                    && (contains(stack, "ProcurementProjectLot") || contains(stack, "Lot"))) {
                                lote.setNumero(value);
                            }

                            if ("Name".equalsIgnoreCase(local) && !value.isBlank()
                                    && (contains(stack, "ProcurementProjectLot") || contains(stack, "Lot"))) {
                                if (isBlank(lote.getTitulo())) lote.setTitulo(value);
                            }

                            if ("ItemClassificationCode".equalsIgnoreCase(local) && !value.isBlank() && inLot) {
                                Cpv cpv = new Cpv();
                                cpv.setCodigo(value);
                                if (lote.getCpvs().isEmpty()) cpv.setPrincipal(true);
                                lote.getCpvs().add(cpv);
                            }

                            if ("TaxExclusiveAmount".equalsIgnoreCase(local) && !value.isBlank()) {
                                lote.setPresupuesto(toMoney(value, currentCurrencyId, false));
                            }
                            if ("TotalAmount".equalsIgnoreCase(local) && !value.isBlank()
                                    && (contains(stack, "LegalMonetaryTotal") || contains(stack, "RequestedTenderTotal"))) {
                                lote.setPresupuesto(toMoney(value, currentCurrencyId, true));
                            }

                            if ("ProcurementProjectLot".equalsIgnoreCase(local) || "Lot".equalsIgnoreCase(local)) {
                                if (notBlank(lote.getTitulo()) || notBlank(lote.getNumero())) {
                                    lic.getLotes().add(lote);
                                }
                                lote = null;
                                inLot = false;
                            }
                        }

                        // -------------------------
                        // ADJUDICACIÓN
                        // -------------------------
                        if (adjud != null) {
                            if (inWinnerParty && "Name".equalsIgnoreCase(local) && contains(stack, "PartyName") && !value.isBlank()) {
                                if (isBlank(adjud.getAdjudicatarioNombre())) adjud.setAdjudicatarioNombre(value);
                            }

                            if (inWinnerParty && "ID".equalsIgnoreCase(local) && contains(stack, "PartyIdentification") && !value.isBlank()) {
                                String scheme = currentIdSchemeName;
                                if ("NIF".equalsIgnoreCase(scheme)) adjud.setAdjudicatarioNif(value);
                            }

                            if (equalsAny(local, "PayableAmount", "TaxInclusiveAmount", "TotalAmount")
                                    && !value.isBlank()
                                    && (contains(stack, "TenderResult") || contains(stack, "AwardedTenderedProject") || contains(stack, "ResultOfTheAward"))) {
                                Boolean incl = null;
                                if ("TaxInclusiveAmount".equalsIgnoreCase(local)) incl = true;
                                adjud.setImporteAdjudicado(toMoney(value, currentCurrencyId, incl));
                            }

                            if ("ReceivedTenderQuantity".equalsIgnoreCase(local) && !value.isBlank()) {
                                Integer n = parseIntSafe(value);
                                if (n != null) adjud.setNumeroOfertas(n);
                            }

                            if ("Description".equalsIgnoreCase(local) && !value.isBlank()
                                    && (contains(stack, "AwardingCriterion") || contains(stack, "AwardingCriteria") || contains(stack, "TenderResult"))) {
                                if (isBlank(adjud.getCriterio())) adjud.setCriterio(value);
                            }

                            if (inWinnerParty && "Party".equalsIgnoreCase(local)) {
                                inWinnerParty = false;
                            }

                            if (equalsAny(local, "TenderResult", "AwardedTenderedProject", "ResultOfTheAward")) {
                                if (notBlank(adjud.getAdjudicatarioNombre()) || adjud.getImporteAdjudicado() != null) {
                                    lic.setAdjudicacion(adjud);
                                }
                            }
                        }

                        // -------------------------
                        // CIERRE ENTRY
                        // -------------------------
                        if ("entry".equalsIgnoreCase(local)) {
                            if (entryXml != null) ev.setRawEntryXml(entryXml.toString());

                            if (notBlank(lic.getExpediente()) || notBlank(ev.getAtomEntryId())) {
                                out.add(new AtomEntryParseResult(lic, ev));
                            }

                            inEntry = false;
                            entryXml = null;
                            lic = null;
                            ev = null;

                            doc = null;
                            lote = null;
                            org = null;
                            adjud = null;

                            inAdditionalDocumentReference = false;
                            inLot = false;
                            inWinnerParty = false;
                        }
                    }

                    // reset attrs cache when element closes
                    if ("ID".equalsIgnoreCase(local)) currentIdSchemeName = null;
                    if ("DurationMeasure".equalsIgnoreCase(local)) currentDurationUnitCode = null;
                    currentCurrencyId = null;

                    stack.pollLast();
                    text.setLength(0);
                }
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException("Error parseando ATOM (StAX)", e);
        }
    }

    // ----------------- helpers -----------------

    private static String attr(XMLStreamReader r, String name) {
        if (r == null) return null;
        if (r.getEventType() != XMLStreamConstants.START_ELEMENT) return null;
        String v = r.getAttributeValue(null, name);
        if (v == null) v = r.getAttributeValue("", name);
        return v;
    }

    private static boolean equalsAny(String s, String... options) {
        for (String o : options) if (o.equalsIgnoreCase(s)) return true;
        return false;
    }

    private static boolean contains(Deque<String> stack, String token) {
        for (String s : stack) if (s.equalsIgnoreCase(token)) return true;
        return false;
    }

    private static boolean isPath(Deque<String> stack, String... seq) {
        if (stack.size() < seq.length) return false;
        Iterator<String> it = stack.descendingIterator();
        for (int i = seq.length - 1; i >= 0; i--) {
            if (!it.hasNext()) return false;
            String s = it.next();
            if (!s.equalsIgnoreCase(seq[i])) return false;
        }
        return true;
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static Instant parseInstant(String s) {
        try { return OffsetDateTime.parse(s).toInstant(); } catch (Exception ignore) {}
        try { return Instant.parse(s); } catch (Exception ignore) {}
        return null;
    }

    private static LocalDate parseLocalDate(String s) {
        try { return LocalDate.parse(s); } catch (Exception ignore) {}
        return null;
    }

    private static Instant parseInstantFromDate(String s) {
        LocalDate d = parseLocalDate(s);
        if (d == null) return null;
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Integer parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception ignore) {}
        return null;
    }

    private static BigDecimal parseDecimalSafe(String s) {
        try { return new BigDecimal(s.trim()); } catch (Exception ignore) {}
        return null;
    }

    private static Money toMoney(String amountText, String currencyId, Boolean incluyeIva) {
        BigDecimal amount = parseDecimalSafe(amountText);
        if (amount == null) return null;
        Money m = new Money();
        m.setAmount(amount);
        m.setCurrency(currencyId != null ? currencyId : "EUR");
        m.setIncluyeIVA(incluyeIva);
        return m;
    }

    private static Boolean parseBooleanLoose(String v) {
        String s = v.trim().toLowerCase(Locale.ROOT);
        if (s.equals("true") || s.equals("1") || s.equals("si") || s.equals("sí") || s.equals("yes")) return true;
        if (s.equals("false") || s.equals("0") || s.equals("no")) return false;
        return null;
    }

    private static TipoContrato mapTipoContrato(String value) {
        String v = value.trim();
        if ("1".equals(v)) return TipoContrato.SUMINISTROS;
        if ("2".equals(v)) return TipoContrato.SERVICIOS;
        if ("3".equals(v)) return TipoContrato.OBRAS;
        try { return TipoContrato.valueOf(v.toUpperCase(Locale.ROOT)); } catch (Exception ignore) { return null; }
    }

    // ---- raw xml helpers ----

    private static void writeStart(XMLStreamReader r, Writer w) throws IOException {
        String prefix = r.getPrefix();
        String local = r.getLocalName();
        String qname = (prefix == null || prefix.isBlank()) ? local : prefix + ":" + local;

        w.write("<");
        w.write(qname);

        // Copia declaraciones de namespace (xmlns / xmlns:prefijo) para que el fragmento sea reparseable.
        // StAX expone estas declaraciones separadas de los atributos normales.
        for (int i = 0; i < r.getNamespaceCount(); i++) {
            String nsPrefix = r.getNamespacePrefix(i); // null o "" para default ns
            String nsURI = r.getNamespaceURI(i);

            w.write(" xmlns");
            if (nsPrefix != null && !nsPrefix.isBlank()) {
                w.write(":" + nsPrefix);
            }
            w.write("=\"");
            w.write(escapeAttr(nsURI));
            w.write("\"");
        }

        for (int i = 0; i < r.getAttributeCount(); i++) {
            String ap = r.getAttributePrefix(i);
            String al = r.getAttributeLocalName(i);
            String aq = (ap == null || ap.isBlank()) ? al : ap + ":" + al;

            String av = r.getAttributeValue(i);

            w.write(" ");
            w.write(aq);
            w.write("=\"");
            w.write(escapeAttr(av));
            w.write("\"");
        }

        w.write(">");
    }

    private static void writeEnd(XMLStreamReader r, Writer w) throws IOException {
        String prefix = r.getPrefix();
        String local = r.getLocalName();
        String qname = (prefix == null || prefix.isBlank()) ? local : prefix + ":" + local;
        w.write("</");
        w.write(qname);
        w.write(">");
    }

    private static String escapeAttr(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeText(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ---- XML sanitize helpers ----

    private static String readAllToString(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64 * 1024);
        byte[] buf = new byte[32 * 1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    /**
     * Sanea el XML para que el parser StAX no aborte por:
     *  - caracteres de control no permitidos por XML 1.0
     *  - '&' sin escapar
     */
    private static String sanitizeXml(String xml) {
        if (xml == null || xml.isEmpty()) return "";

        // 1) eliminar caracteres no válidos en XML 1.0
        StringBuilder sb = new StringBuilder(xml.length());
        for (int i = 0; i < xml.length(); i++) {
            char c = xml.charAt(i);
            // Permitidos: TAB(0x9), LF(0xA), CR(0xD), y >= 0x20
            if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) {
                sb.append(c);
            }
        }

        // 2) escapar '&' sueltos
        return UNESCAPED_AMP.matcher(sb).replaceAll("&amp;");
    }
}

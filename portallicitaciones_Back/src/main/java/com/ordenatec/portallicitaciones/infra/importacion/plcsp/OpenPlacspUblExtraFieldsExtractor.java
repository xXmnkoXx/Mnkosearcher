package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import javax.xml.stream.*;
import java.io.StringReader;
import java.util.*;

/**
 * Extrae campos que OpenPLACSP suele rellenar pero que normalmente NO están en tu modelo
 * (directiva, sistema contratación, subasta electrónica, forma presentación, subcontratación, etc.)
 * leyendo el XML crudo (evento.rawContentXml / rawEntryXml) con StAX y comparando por localName
 * (ignora namespaces).
 *
 * OJO: PLCSP usa UBL + CAC/CBC, y algunos portales varían tags.
 * Este extractor es "best effort": intenta múltiples rutas/nombres para cada campo.
 */
public class OpenPlacspUblExtraFieldsExtractor {

    public ExtraFields extract(String rawXml) {
        if (rawXml == null || rawXml.trim().isBlank()) {
            return ExtraFields.empty();
        }

        // Parseamos y guardamos:
        // - firstByName: primer texto no vacío por localName
        // - allByName: todos los valores por localName (para joins como CPV, etc.)
        ParsedXml parsed = parse(rawXml);

        ExtraFields f = new ExtraFields();

        // ---------------------------
        // Directiva de aplicación
        // ---------------------------
        // Variantes típicas UBL/ES:
        //  - Directive / ApplicableDirective / EUDirective / ProcurementLegislationDocumentReference ...
        f.directivaAplicacion = firstNonBlank(
                parsed.first("ApplicableDirective"),
                parsed.first("Directive"),
                parsed.first("ProcurementDirective"),
                parsed.first("EUProcurementDirective"),
                parsed.first("RegulatoryDomain"),
                parsed.first("LegalReference") // fallback
        );

        // ---------------------------
        // Sistema de contratación
        // ---------------------------
        // Variantes:
        //  - ContractingSystemTypeCode
        //  - ContractingSystem
        //  - ContractingSystemType
        f.sistemaContratacion = firstNonBlank(
                parsed.first("ContractingSystemTypeCode"),
                parsed.first("ContractingSystemType"),
                parsed.first("ContractingSystem")
        );

        // ---------------------------
        // Tramitación (ordinaria/urgente/emergencia…)
        // ---------------------------
        // Variantes:
        //  - UrgencyCode
        //  - ProcessUrgency
        //  - ProcedureUrgency
        f.tramitacion = firstNonBlank(
                parsed.first("UrgencyCode"),
                parsed.first("ProcessUrgency"),
                parsed.first("ProcedureUrgency")
        );

        // ---------------------------
        // Forma de presentación de la oferta
        // ---------------------------
        // Variantes:
        //  - SubmissionMethodCode (electrónica/presencial/mixta)
        //  - TenderSubmissionMethod
        //  - SubmissionMethod
        f.formaPresentacionOferta = firstNonBlank(
                parsed.first("SubmissionMethodCode"),
                parsed.first("TenderSubmissionMethod"),
                parsed.first("SubmissionMethod"),
                parsed.first("PresentationMethod")
        );

        // ---------------------------
        // Subasta electrónica (sí/no)
        // ---------------------------
        // Variantes:
        //  - AuctionConstraintIndicator (true/false)
        //  - ElectronicAuctionIndicator
        //  - AuctionIndicator
        f.subastaElectronica = toYesNo(firstNonBlank(
                parsed.first("AuctionConstraintIndicator"),
                parsed.first("ElectronicAuctionIndicator"),
                parsed.first("AuctionIndicator")
        ));

        // ---------------------------
        // Subcontratación permitida + porcentaje
        // ---------------------------
        // Variantes:
        //  - AllowedSubcontractTerms -> Indicator / Text / Percent
        //  - SubcontractingConditions -> Indicator / Percent
        //  - SubcontractingAllowedIndicator
        //  - SubcontractingPercentage
        String subAllowedRaw = firstNonBlank(
                parsed.first("SubcontractingAllowedIndicator"),
                parsed.first("AllowedSubcontractIndicator"),
                parsed.first("AllowedSubcontractTermsIndicator"),
                parsed.first("SubcontractingAllowed")
        );

        // A veces solo hay texto tipo "Se permite subcontratación" en un nodo genérico
        if (isBlank(subAllowedRaw)) {
            // Intento por heurística: buscar textos con palabras clave
            subAllowedRaw = parsed.firstContainsText("subcontrat");
        }

        f.subcontratacionPermitida = toYesNo(subAllowedRaw);

        // Porcentaje
        // Variantes:
        //  - MaximumPercent / Percent / SubcontractingPercentage / MaximumSubcontractingPercentage
        f.subcontratacionPorcentaje = firstNonBlank(
                parsed.first("SubcontractingPercentage"),
                parsed.first("MaximumSubcontractingPercentage"),
                parsed.first("MaximumPercent"),
                parsed.first("Percent")
        );

        // ---------------------------
        // Financiación Europea y fuente + descripción
        // ---------------------------
        // Variantes:
        //  - FundingProgram / FundingProgramme / EUFunds / Financing
        //  - ProjectReference / Programme / FundingSource
        f.financiacionEuropeaYFuente = firstNonBlank(
                parsed.first("FundingProgram"),
                parsed.first("FundingProgramme"),
                parsed.first("FundingSource"),
                parsed.first("EUFunds"),
                parsed.first("Financing")
        );

        // Descripción financiación
        f.descripcionFinanciacionEuropea = firstNonBlank(
                parsed.first("FundingDescription"),
                parsed.first("FundingText"),
                parsed.first("FinancingDescription"),
                parsed.first("ProjectDescription")
        );

        // ---------------------------
        // Identificador único TED (si aparece)
        // ---------------------------
        // Variantes:
        //  - TED (o IDs tipo "2026/S 012-xxxxx")
        //  - OfficialJournalNumber / TEDIdentifier / OJNumber
        String ted = firstNonBlank(
                parsed.first("TEDIdentifier"),
                parsed.first("OfficialJournalNumber"),
                parsed.first("OJNumber"),
                parsed.first("OfficialJournalDocumentReference")
        );
        if (isBlank(ted)) {
            ted = parsed.firstMatchesRegex("\\d{4}/S\\s*\\d{3}-\\d+"); // ej: 2026/S 012-123456
        }
        f.identificadorTed = ted;

        return f;
    }

    // ============================================================
    // Model
    // ============================================================

    public static final class ExtraFields {
        public String directivaAplicacion;
        public String sistemaContratacion;
        public String tramitacion;
        public String formaPresentacionOferta;
        public String subastaElectronica; // "Sí" / "No" / null
        public String subcontratacionPermitida; // "Sí" / "No" / null
        public String subcontratacionPorcentaje; // texto original (ej "30" o "30%")
        public String financiacionEuropeaYFuente;
        public String descripcionFinanciacionEuropea;
        public String identificadorTed;

        public static ExtraFields empty() {
            return new ExtraFields();
        }
    }

    // ============================================================
    // Parsing (StAX, namespace-agnostic)
    // ============================================================

    private static final class ParsedXml {
        final Map<String, String> firstByName = new HashMap<>();
        final Map<String, List<String>> allByName = new HashMap<>();
        final List<String> allTexts = new ArrayList<>();

        String first(String localName) {
            return firstByName.get(localName);
        }

        String firstContainsText(String needleLower) {
            if (needleLower == null) return null;
            String n = needleLower.toLowerCase(Locale.ROOT);
            for (String t : allTexts) {
                if (t == null) continue;
                String tl = t.toLowerCase(Locale.ROOT);
                if (tl.contains(n)) return t;
            }
            return null;
        }

        String firstMatchesRegex(String regex) {
            if (regex == null) return null;
            for (String t : allTexts) {
                if (t != null && t.matches(".*" + regex + ".*")) return t;
            }
            return null;
        }

        void put(String localName, String value) {
            if (isBlank(localName) || isBlank(value)) return;

            String v = value.trim();
            allByName.computeIfAbsent(localName, k -> new ArrayList<>()).add(v);
            allTexts.add(v);

            // Guardar primer valor no vacío
            firstByName.putIfAbsent(localName, v);
        }
    }

    private ParsedXml parse(String xml) {
        ParsedXml out = new ParsedXml();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Seguridad + robustez
        trySet(factory, XMLInputFactory.SUPPORT_DTD, false);
        trySet(factory, "javax.xml.stream.isSupportingExternalEntities", false);
        trySet(factory, XMLInputFactory.IS_COALESCING, true);

        Deque<String> stack = new ArrayDeque<>();
        StringBuilder text = new StringBuilder();

        try (StringReader sr = new StringReader(xml)) {
            XMLStreamReader r = factory.createXMLStreamReader(sr);

            while (r.hasNext()) {
                int ev = r.next();

                if (ev == XMLStreamConstants.START_ELEMENT) {
                    stack.push(local(r));
                    text.setLength(0);

                    // También miramos atributos relevantes (por si alguno trae codes)
                    int ac = r.getAttributeCount();
                    for (int i = 0; i < ac; i++) {
                        String an = r.getAttributeLocalName(i);
                        String av = r.getAttributeValue(i);
                        // guardamos atributos como si fueran "LocalName@attr"
                        if (!isBlank(an) && !isBlank(av)) {
                            out.put(stack.peek() + "@" + an, av);
                        }
                    }
                }

                if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                    if (!r.isWhiteSpace()) {
                        text.append(r.getText());
                    }
                }

                if (ev == XMLStreamConstants.END_ELEMENT) {
                    String current = local(r);
                    String value = text.toString();

                    // Guardamos por localName del elemento
                    out.put(current, value);

                    // Heurística adicional: si hay stack y current coincide, pop
                    if (!stack.isEmpty() && Objects.equals(stack.peek(), current)) {
                        stack.pop();
                    }
                    text.setLength(0);
                }
            }

        } catch (Exception ignored) {
            // Si falla el parseo, devolvemos vacío (best effort)
            return new ParsedXml();
        }

        return out;
    }

    private static String local(XMLStreamReader r) {
        String ln = r.getLocalName();
        return ln != null ? ln.trim() : null;
    }

    private static void trySet(XMLInputFactory f, String key, Object val) {
        try {
            f.setProperty(key, val);
        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isBlank();
    }

    private static String toYesNo(String raw) {
        if (isBlank(raw)) return null;
        String v = raw.trim().toLowerCase(Locale.ROOT);

        // valores típicos XML booleanos
        if (v.equals("true") || v.equals("1") || v.equals("sí") || v.equals("si") || v.equals("yes")) return "Sí";
        if (v.equals("false") || v.equals("0") || v.equals("no")) return "No";

        // heurística por texto
        if (v.contains("no se permite") || v.contains("no permitido") || v.contains("no permitida")) return "No";
        if (v.contains("se permite") || v.contains("permitida") || v.contains("permitido") || v.contains("allowed")) return "Sí";

        // si no sabemos, devolvemos el raw (pero idealmente no llenaríamos una col Sí/No con basura)
        // aquí preferimos null para no ensuciar
        return null;
    }
}

package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exportador HEADLESS ATOM -> XLSX (OpenPLACSP-like).
 *
 * Objetivo:
 * - Rellenar cabecera completa (licitación)
 * - Expandir lotes (1 fila por lote)
 * - Expandir adjudicaciones/resultados (1 fila por lote x adjudicación)
 *
 * Nota:
 * El ATOM PLCSP usa namespaces; aquí trabajamos por localName (ignoramos prefijos).
 */
@Component
public class OpenPlacspAtomExporter {

    private static final Logger log = LoggerFactory.getLogger(OpenPlacspAtomExporter.class);

    private final XMLInputFactory xmlFactory;

    public OpenPlacspAtomExporter() {
        this.xmlFactory = XMLInputFactory.newInstance();
        try { this.xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false); } catch (Exception ignored) {}
        try { this.xmlFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false); } catch (Exception ignored) {}
    }

    public void export(Path atomRootFile, Path xlsxFile) {
        Objects.requireNonNull(atomRootFile, "atomRootFile");
        Objects.requireNonNull(xlsxFile, "xlsxFile");

        if (!Files.exists(atomRootFile)) {
            throw new IllegalArgumentException("ATOM no existe: " + atomRootFile.toAbsolutePath());
        }

        List<Path> atomFiles = resolveAllAtomFilesSameDir(atomRootFile);

        log.info("OpenPLACSP HEADLESS: root={}", atomRootFile.getFileName());
        log.info("OpenPLACSP HEADLESS: atom files detectados={}", atomFiles.size());

        int totalRows = 0;

        try (OpenPlacspXlsxWriter xw = new OpenPlacspXlsxWriter()) {

            OpenPlacspRowWriter.SheetRowWriter w = OpenPlacspRowWriter.forHeaders(
                    xw.licitacionesSheet(),
                    OpenPlacspXlsxSchema.LICITACIONES_HEADERS,
                    xw.textStyle(),
                    xw.numberStyle(),
                    xw.dateStyle(),
                    xw.dateTimeStyle()
            );

            for (int i = 0; i < atomFiles.size(); i++) {
                Path atom = atomFiles.get(i);
                int n = exportOneAtom(atom, w);
                totalRows += n;

                if (i == 0 || (i + 1) % 10 == 0) {
                    log.info("OpenPLACSP HEADLESS: progreso {}/{} atoms, filas acumuladas={}",
                            (i + 1), atomFiles.size(), totalRows);
                }
            }

            xw.writeTo(xlsxFile);

            log.info("OpenPLACSP HEADLESS: export completado. filasTotal={}, out={}",
                    totalRows, xlsxFile.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("OpenPLACSP HEADLESS export error: " + e.getMessage(), e);
        }
    }

    private int exportOneAtom(Path atomFile, OpenPlacspRowWriter.SheetRowWriter w) {
        int rows = 0;

        try (InputStream in = new BufferedInputStream(Files.newInputStream(atomFile))) {
            XMLStreamReader r = xmlFactory.createXMLStreamReader(in, StandardCharsets.UTF_8.name());

            boolean inEntry = false;
            Deque<String> stack = new ArrayDeque<>();
            String textBuf = null;

            // ====== Entry state ======
            Map<String, String> base = null;                 // cabecera licitación
            List<Lot> lots = null;                           // lotes de la licitación
            Lot currentLot = null;                           // lote actual
            Award currentAward = null;                       // adjudicación/result actual
            String lastIdSchemeName = null;                  // schemeName para ID (NIF/DIR3)
            String lastAmountCurrency = null;                // moneda (si aparece como atributo)
            String lastCpvScheme = null;                     // por si viene scheme en CPV

            while (r.hasNext()) {
                int ev = r.next();

                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String local = local(r);

                    if ("entry".equalsIgnoreCase(local)) {
                        inEntry = true;
                        stack.clear();
                        textBuf = null;

                        base = new LinkedHashMap<>();
                        lots = new ArrayList<>();
                        currentLot = null;
                        currentAward = null;
                        lastIdSchemeName = null;
                        lastAmountCurrency = null;
                        lastCpvScheme = null;
                        continue;
                    }

                    if (!inEntry) continue;

                    // Atom básicos
                    if ("id".equalsIgnoreCase(local)) {
                        putIfNotBlank(base, "Identificador", readElementTextSafe(r));
                        continue;
                    }
                    if ("title".equalsIgnoreCase(local)) {
                        // a veces coincide con objeto
                        putIfNotBlank(base, "Objeto del Contrato", readElementTextSafe(r));
                        continue;
                    }
                    if ("updated".equalsIgnoreCase(local)) {
                        putIfNotBlank(base, "Fecha actualización", readElementTextSafe(r));
                        continue;
                    }
                    if ("published".equalsIgnoreCase(local)) {
                        putIfNotBlank(base, "Primera publicación", readElementTextSafe(r));
                        continue;
                    }
                    if ("link".equalsIgnoreCase(local)) {
                        String rel = attr(r, "rel");
                        String href = attr(r, "href");
                        if (!isBlank(href) && (isBlank(rel) || "alternate".equalsIgnoreCase(rel))) {
                            putIfNotBlank(base, "Link licitación", href.trim());
                        }
                        continue;
                    }

                    // Stack general (para mapear por paths)
                    stack.addLast(local);

                    // Detectores de “contexto” (lote / adjudicación)
                    if ("ProcurementProjectLot".equalsIgnoreCase(local) || "ProcurementProjectLotType".equalsIgnoreCase(local)) {
                        // abre un lote
                        currentLot = new Lot();
                        lots.add(currentLot);
                    }

                    if ("TenderResult".equalsIgnoreCase(local) || "TenderResultType".equalsIgnoreCase(local)
                            || "AwardResult".equalsIgnoreCase(local) || "AwardResultType".equalsIgnoreCase(local)) {
                        // abre una adjudicación/result
                        if (currentLot == null) {
                            // Hay feeds que ponen TenderResult a nivel de entry (sin lote) -> creamos lote "sin nombre"
                            currentLot = new Lot();
                            lots.add(currentLot);
                        }
                        currentAward = new Award();
                        currentLot.awards.add(currentAward);
                    }

                    // Atributos útiles
                    if ("ID".equalsIgnoreCase(local)) {
                        String scheme = attrAnyNs(r, "schemeName");
                        if (!isBlank(scheme)) lastIdSchemeName = scheme.trim();
                    }
                    if ("Amount".equalsIgnoreCase(local) || "TaxInclusiveAmount".equalsIgnoreCase(local) || "TaxExclusiveAmount".equalsIgnoreCase(local)) {
                        String cur = attrAnyNs(r, "currencyID");
                        if (!isBlank(cur)) lastAmountCurrency = cur.trim();
                    }
                    if ("ItemClassificationCode".equalsIgnoreCase(local)) {
                        String scheme = attrAnyNs(r, "listID");
                        if (!isBlank(scheme)) lastCpvScheme = scheme.trim();
                    }

                } else if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                    if (!inEntry) continue;
                    String t = r.getText();
                    if (t != null) {
                        t = t.trim();
                        if (!t.isEmpty()) textBuf = t;
                    }

                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String local = local(r);

                    if (!inEntry) continue;

                    if ("entry".equalsIgnoreCase(local)) {
                        // 1) si no hay lotes: una fila base
                        if (lots == null || lots.isEmpty()) {
                            writeRow(w, base, null, null);
                            rows++;
                        } else {
                            // 2) expandir:
                            for (Lot lot : lots) {
                                if (lot.awards.isEmpty()) {
                                    writeRow(w, base, lot, null);
                                    rows++;
                                } else {
                                    for (Award a : lot.awards) {
                                        writeRow(w, base, lot, a);
                                        rows++;
                                    }
                                }
                            }
                        }

                        // reset entry
                        inEntry = false;
                        base = null;
                        lots = null;
                        currentLot = null;
                        currentAward = null;
                        stack.clear();
                        textBuf = null;
                        lastIdSchemeName = null;
                        lastAmountCurrency = null;
                        lastCpvScheme = null;
                        continue;
                    }

                    // Mapear valores cuando cerramos tags dentro del entry
                    if (!isBlank(textBuf)) {
                        String path = String.join("/", stack);
                        applyMapping(path, local, textBuf, lastIdSchemeName, lastAmountCurrency, lastCpvScheme, base, currentLot, currentAward);
                    }

                    // cierres de contexto
                    if ("TenderResult".equalsIgnoreCase(local) || "AwardResult".equalsIgnoreCase(local)
                            || "TenderResultType".equalsIgnoreCase(local) || "AwardResultType".equalsIgnoreCase(local)) {
                        currentAward = null;
                    }

                    if ("ProcurementProjectLot".equalsIgnoreCase(local) || "ProcurementProjectLotType".equalsIgnoreCase(local)) {
                        currentLot = null;
                    }

                    if ("ID".equalsIgnoreCase(local)) lastIdSchemeName = null;
                    if ("Amount".equalsIgnoreCase(local) || "TaxInclusiveAmount".equalsIgnoreCase(local) || "TaxExclusiveAmount".equalsIgnoreCase(local)) {
                        lastAmountCurrency = null;
                    }
                    if ("ItemClassificationCode".equalsIgnoreCase(local)) lastCpvScheme = null;

                    // pop stack
                    if (!stack.isEmpty() && stack.getLast().equalsIgnoreCase(local)) {
                        stack.removeLast();
                    } else {
                        // desync raro -> limpiamos
                        stack.clear();
                    }

                    textBuf = null;
                }
            }

        } catch (Exception e) {
            log.warn("OpenPLACSP HEADLESS: error en {}: {}", atomFile.getFileName(), e.toString());
        }

        return rows;
    }

    /**
     * Escribe fila final con merge:
     * - Base: cabeceras generales
     * - Lot: columnas lote
     * - Award: columnas resultado/adjudicación/contrato
     */
    private void writeRow(OpenPlacspRowWriter.SheetRowWriter w, Map<String, String> base, Lot lot, Award award) {
        Row row = w.newRow();

        // 1) base general
        writeHeaders(w, row, base);

        // 2) lote
        if (lot != null) {
            Map<String, String> lotMap = lot.toHeaders();
            writeHeaders(w, row, lotMap);
        }

        // 3) award
        if (award != null) {
            Map<String, String> awMap = award.toHeaders();
            writeHeaders(w, row, awMap);
        }
    }

    private void writeHeaders(OpenPlacspRowWriter.SheetRowWriter w, Row row, Map<String, String> values) {
        if (values == null || values.isEmpty()) return;
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (!isBlank(e.getValue())) {
                w.setText(row, e.getKey(), e.getValue());
            }
        }
    }

    /**
     * Mapeo flexible por paths/localName (ignora namespaces).
     *
     * Reglas:
     * - Si estamos dentro de lote (currentLot != null), algunos campos se guardan a nivel lote
     * - Si estamos dentro de adjudicación (currentAward != null), algunos campos se guardan a nivel award
     */
    private static void applyMapping(
            String path,
            String closingLocal,
            String rawValue,
            String idScheme,
            String amountCurrency,
            String cpvScheme,
            Map<String, String> base,
            Lot lot,
            Award award
    ) {
        if (isBlank(rawValue)) return;
        String v = rawValue.trim();

        boolean inLot = lot != null;
        boolean inAward = award != null;

        // =========================
        // 1) CABECERA (licitación)
        // =========================
        // Expediente
        if (endsWithAny(path, "ContractFolderStatus/ContractFolderID", "ContractFolderID")) {
            putIfNotBlank(base, "Número de expediente", v);
            return;
        }

        // Estado
        if (endsWithAny(path, "ContractFolderStatusCode", "ContractFolderStatus/ContractFolderStatusCode")) {
            putIfNotBlank(base, "Estado", v);
            return;
        }

        // Perfil OC
        if (endsWithAny(path, "BuyerProfileURIID")) {
            putIfNotBlank(base, "Enlace al Perfil de Contratante del OC", v);
            return;
        }

        // Órgano contratación (name)
        if (endsWithAny(path, "LocatedContractingParty/Party/PartyName/Name", "PartyName/Name")) {
            // Solo si no estamos en adjudicatario (que también tiene PartyName/Name)
            if (!path.contains("WinningParty") && !path.contains("AwardedTenderedProject") && !path.contains("EconomicOperatorParty")) {
                putIfNotBlank(base, "Órgano de Contratación", v);
                return;
            }
        }

        // Identificadores OC
        if (endsWithAny(path, "PartyIdentification/ID", "Party/PartyIdentification/ID")) {
            // si estamos en adjudicatario, lo maneja abajo
            if (!path.contains("WinningParty") && !path.contains("EconomicOperatorParty")) {
                String scheme = idScheme != null ? idScheme.trim().toUpperCase(Locale.ROOT) : "";
                if ("NIF".equals(scheme)) {
                    putIfNotBlank(base, "NIF OC", v);
                    return;
                }
                if ("DIR3".equals(scheme)) {
                    putIfNotBlank(base, "DIR3", v);
                    return;
                }
                putIfNotBlank(base, "ID OC en PLACSP", v);
                return;
            }
        }

        // Tipo administración
        if (endsWithAny(path, "ContractingPartyTypeCode")) {
            putIfNotBlank(base, "Tipo de Administración", v);
            return;
        }

        // Tipo contrato / procedimiento / tramitación / sistema
        if (endsWithAny(path, "ContractTypeCode")) {
            putIfNotBlank(base, "Tipo de contrato", v);
            return;
        }
        if (endsWithAny(path, "ProcedureCode")) {
            putIfNotBlank(base, "Tipo de procedimiento", v);
            return;
        }
        if (endsWithAny(path, "UrgencyCode")) {
            putIfNotBlank(base, "Tramitación", v);
            return;
        }
        if (endsWithAny(path, "ContractingSystemCode")) {
            putIfNotBlank(base, "Sistema de contratación", v);
            return;
        }

        // Subasta / subcontratación
        if (endsWithAny(path, "AuctionConstraintIndicator")) {
            putIfNotBlank(base, "Subasta electrónica", v);
            return;
        }
        if (endsWithAny(path, "SubcontractingConditions", "SubcontractingConditionsCode")) {
            putIfNotBlank(base, "Subcontratación permitida", v);
            return;
        }
        if (endsWithAny(path, "SubcontractingConditions/MaximumPercent", "MaximumPercent")) {
            // solo si estamos en ese ámbito
            if (path.contains("Subcontract")) {
                putIfNotBlank(base, "Subcontratación permitida porcentaje", v);
                return;
            }
        }

        // Objeto (a veces viene como ProcurementProject/Name)
        if (endsWithAny(path, "ProcurementProject/Name")) {
            putIfNotBlank(base, "Objeto del Contrato", v);
            return;
        }

        // CPV cabecera (si no estamos en lote)
        if (!inLot && endsWithAny(path, "ItemClassificationCode", "RequiredCommodityClassification/ItemClassificationCode")) {
            appendCsv(base, "CPV", v);
            return;
        }

        // Importes cabecera
        if (!inLot && endsWithAny(path, "EstimatedOverallContractAmount/Amount", "EstimatedOverallContractAmount")) {
            putIfNotBlank(base, "Valor estimado del contrato", decorateAmount(v, amountCurrency));
            return;
        }
        if (!inLot && endsWithAny(path, "TaxExclusiveAmount")) {
            putIfNotBlank(base, "Presupuesto base sin impuestos", decorateAmount(v, amountCurrency));
            return;
        }
        if (!inLot && endsWithAny(path, "TaxInclusiveAmount")) {
            putIfNotBlank(base, "Presupuesto base con impuestos", decorateAmount(v, amountCurrency));
            return;
        }

        // Lugar ejecución cabecera
        if (!inLot && endsWithAny(path, "RealizedLocation/Address/PostalZone")) {
            putIfNotBlank(base, "Código Postal", v);
            return;
        }
        if (!inLot && (endsWithAny(path, "RealizedLocation/Address/ID") || endsWithAny(path, "RealizedLocation/Address/CountrySubentity"))) {
            putIfNotBlank(base, "Lugar de ejecución", v);
            return;
        }

        // Fechas presentación (cabecera)
        if (!inLot && path.contains("TenderSubmissionDeadlinePeriod") && closingLocal.equalsIgnoreCase("EndDate")) {
            putIfNotBlank(base, "Fecha de presentación de ofertas", v);
            return;
        }
        if (!inLot && path.contains("ParticipationRequestReceptionPeriod") && closingLocal.equalsIgnoreCase("EndDate")) {
            putIfNotBlank(base, "Fecha de presentación de solicitudes de participacion", v);
            return;
        }

        // Moneda (si aparece explícita)
        if (!isBlank(amountCurrency)) {
            putIfNotBlank(base, "Moneda", amountCurrency);
        }

        // =========================
        // 2) LOTE
        // =========================
        if (inLot && award == null) {
            // Lote ID / Número
            if (endsWithAny(path, "ProcurementProjectLot/ID", "ProcurementProjectLotType/ID", "ProcurementProjectLot/ID/ID")) {
                lot.lote = v;
                return;
            }

            // Objeto lote
            if (path.contains("ProcurementProjectLot") && endsWithAny(path, "Name")) {
                lot.objeto = preferLonger(lot.objeto, v);
                return;
            }

            // CPV lote
            if (path.contains("ProcurementProjectLot") && endsWithAny(path, "ItemClassificationCode", "RequiredCommodityClassification/ItemClassificationCode")) {
                lot.cpv = appendCsvStr(lot.cpv, v);
                return;
            }

            // Lugar ejecución lote
            if (path.contains("ProcurementProjectLot") && endsWithAny(path, "RealizedLocation/Address/PostalZone")) {
                lot.lugar = preferLonger(lot.lugar, v);
                return;
            }
            if (path.contains("ProcurementProjectLot") && (endsWithAny(path, "RealizedLocation/Address/ID") || endsWithAny(path, "RealizedLocation/Address/CountrySubentity"))) {
                lot.lugar = preferLonger(lot.lugar, v);
                return;
            }

            // Importes lote
            if (path.contains("ProcurementProjectLot") && endsWithAny(path, "EstimatedOverallContractAmount/Amount", "EstimatedOverallContractAmount")) {
                lot.valorEstimado = decorateAmount(v, amountCurrency);
                return;
            }
            if (path.contains("ProcurementProjectLot") && endsWithAny(path, "TaxExclusiveAmount")) {
                lot.presSinImp = decorateAmount(v, amountCurrency);
                return;
            }
            if (path.contains("ProcurementProjectLot") && endsWithAny(path, "TaxInclusiveAmount")) {
                lot.presConImp = decorateAmount(v, amountCurrency);
                return;
            }
        }

        // =========================
        // 3) RESULTADO / ADJUDICACIÓN / CONTRATO
        // =========================
        if (inAward) {
            // Resultado (código/estado)
            if (endsWithAny(path, "TenderResult/ResultCode", "AwardResult/ResultCode", "ResultCode")) {
                award.resultado = v;
                return;
            }

            // Fecha acuerdo / adjudicación (AwardDate / IssueDate)
            if (endsWithAny(path, "AwardDate", "TenderResult/AwardDate", "AwardResult/AwardDate")) {
                award.fechaAcuerdo = v;
                return;
            }

            // Nº ofertas recibidas
            if (endsWithAny(path, "ReceivedTenderQuantity", "TenderResult/ReceivedTenderQuantity")) {
                award.numOfertas = v;
                return;
            }

            // Importes oferta baja / alta (si vienen)
            if (endsWithAny(path, "LowerTenderAmount", "LowerTenderAmount/Amount")) {
                award.precioBajo = decorateAmount(v, amountCurrency);
                return;
            }
            if (endsWithAny(path, "HigherTenderAmount", "HigherTenderAmount/Amount")) {
                award.precioAlto = decorateAmount(v, amountCurrency);
                return;
            }

            // Anormalmente bajas (indicator)
            if (endsWithAny(path, "AbnormallyLowTenderIndicator", "AbnormallyLowTenderIndicator/Indicator")) {
                award.anormalBaja = v;
                return;
            }

            // Nº contrato / fechas formalización / entrada vigor
            if (endsWithAny(path, "Contract/ID", "ContractID", "Contract/ID/ID")) {
                award.numContrato = v;
                return;
            }
            if (endsWithAny(path, "Contract/IssueDate", "IssueDate")) {
                award.fechaFormalizacion = v;
                return;
            }
            if (endsWithAny(path, "Contract/StartDate", "StartDate")) {
                award.fechaEntradaVigor = v;
                return;
            }

            // Adjudicatario (nombre)
            if (path.contains("WinningParty") && endsWithAny(path, "PartyName/Name")) {
                award.adjudicatario = preferLonger(award.adjudicatario, v);
                return;
            }

            // Adjudicatario ID (NIF)
            if (path.contains("WinningParty") && endsWithAny(path, "PartyIdentification/ID")) {
                // schemeName a veces te dice NIF/DUNS/etc
                String scheme = idScheme != null ? idScheme.trim() : null;
                if (!isBlank(scheme) && isBlank(award.tipoIdAdjud)) award.tipoIdAdjud = scheme;
                award.idAdjud = v;
                return;
            }

            // PYME indicator
            if (endsWithAny(path, "SMEIndicator", "SMEIndicator/Indicator")) {
                award.esPyme = v;
                return;
            }

            // Importes adjudicación (sin/con imp)
            if (endsWithAny(path, "AwardedTenderedProject/LegalMonetaryTotal/TaxExclusiveAmount", "TaxExclusiveAmount")) {
                // OJO: aquí también aparece en otros sitios; lo aceptamos como "importe sin impuestos"
                award.impSin = decorateAmount(v, amountCurrency);
                return;
            }
            if (endsWithAny(path, "AwardedTenderedProject/LegalMonetaryTotal/TaxInclusiveAmount", "TaxInclusiveAmount")) {
                award.impCon = decorateAmount(v, amountCurrency);
                return;
            }
        }
    }

    // =========================
    // Models -> headers
    // =========================

    private static class Lot {
        String lote;
        String objeto;
        String valorEstimado;
        String presConImp;
        String presSinImp;
        String cpv;
        String lugar;
        final List<Award> awards = new ArrayList<>();

        Map<String, String> toHeaders() {
            Map<String, String> m = new LinkedHashMap<>();
            putIfNotBlank(m, "Lote", lote);
            putIfNotBlank(m, "Objeto licitación/lote", objeto);
            putIfNotBlank(m, "Valor estimado licitación/lote", valorEstimado);
            putIfNotBlank(m, "Presupuesto base con impuestos licitación/lote", presConImp);
            putIfNotBlank(m, "Presupuesto base sin impuestos licitación/lote", presSinImp);
            putIfNotBlank(m, "CPV licitación/lote", cpv);
            putIfNotBlank(m, "Lugar ejecución licitación/lote", lugar);
            return m;
        }
    }

    private static class Award {
        String resultado;
        String fechaAcuerdo;
        String numOfertas;
        String precioBajo;
        String precioAlto;
        String anormalBaja;

        String numContrato;
        String fechaFormalizacion;
        String fechaEntradaVigor;

        String adjudicatario;
        String tipoIdAdjud;
        String idAdjud;
        String esPyme;

        String impSin;
        String impCon;

        Map<String, String> toHeaders() {
            Map<String, String> m = new LinkedHashMap<>();
            putIfNotBlank(m, "Resultado licitación/lote", resultado);
            putIfNotBlank(m, "Fecha del acuerdo licitación/lote", fechaAcuerdo);
            putIfNotBlank(m, "Número de ofertas recibidas por licitación/lote", numOfertas);
            putIfNotBlank(m, "Precio de la oferta más baja por licitación/lote", precioBajo);
            putIfNotBlank(m, "Precio de la oferta más alta por licitación/lote", precioAlto);
            putIfNotBlank(m, "Se han excluído ofertas por ser anormalmente bajas por licitación/lote", anormalBaja);

            putIfNotBlank(m, "Número del contrato licitación/lote", numContrato);
            putIfNotBlank(m, "Fecha formalización del contrato licitación/lote", fechaFormalizacion);
            putIfNotBlank(m, "Fecha entrada en vigor del contrato de licitación/lote", fechaEntradaVigor);

            putIfNotBlank(m, "Adjudicatario licitación/lote", adjudicatario);
            putIfNotBlank(m, "Tipo de identificador de adjudicatario por licitación/lote", tipoIdAdjud);
            putIfNotBlank(m, "Identificador Adjudicatario de la licitación/lote", idAdjud);
            putIfNotBlank(m, "El adjudicatario es o no PYME de la licitación/lote", esPyme);

            putIfNotBlank(m, "Importe adjudicación sin impuestos licitación/lote", impSin);
            putIfNotBlank(m, "Importe adjudicación con impuestos licitación/lote", impCon);
            return m;
        }
    }

    // =========================
    // Helpers
    // =========================

    private static void putIfNotBlank(Map<String, String> m, String k, String v) {
        if (!isBlank(v)) m.put(k, v.trim());
    }

    private static void appendCsv(Map<String, String> m, String k, String v) {
        if (isBlank(v)) return;
        String cur = m.get(k);
        String val = v.trim();
        if (isBlank(cur)) m.put(k, val);
        else if (!Arrays.asList(cur.split(",")).contains(val)) m.put(k, cur + "," + val);
    }

    private static String appendCsvStr(String cur, String v) {
        if (isBlank(v)) return cur;
        String val = v.trim();
        if (isBlank(cur)) return val;
        List<String> parts = Arrays.asList(cur.split(","));
        if (parts.contains(val)) return cur;
        return cur + "," + val;
    }

    private static String decorateAmount(String amount, String currency) {
        if (isBlank(amount)) return null;
        if (isBlank(currency)) return amount.trim();
        // lo dejamos como texto "12345.67 EUR" para el XLSX
        return amount.trim() + " " + currency.trim();
    }

    private static String preferLonger(String a, String b) {
        if (isBlank(a)) return b;
        if (isBlank(b)) return a;
        return b.length() > a.length() ? b : a;
    }

    private static boolean endsWithAny(String path, String... suffixes) {
        if (path == null) return false;
        for (String suf : suffixes) if (path.endsWith(suf)) return true;
        return false;
    }

    private static String local(XMLStreamReader r) {
        try {
            if (r.getName() != null && r.getName().getLocalPart() != null) return r.getName().getLocalPart();
        } catch (Exception ignored) {}
        try {
            return r.getLocalName();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String attr(XMLStreamReader r, String name) {
        try {
            String v = r.getAttributeValue(null, name);
            return v == null ? null : v.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static String attrAnyNs(XMLStreamReader r, String localName) {
        try {
            for (int i = 0; i < r.getAttributeCount(); i++) {
                String ln = r.getAttributeLocalName(i);
                if (ln != null && ln.equalsIgnoreCase(localName)) {
                    String v = r.getAttributeValue(i);
                    return v == null ? null : v.trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String readElementTextSafe(XMLStreamReader r) {
        try {
            String t = r.getElementText();
            return t == null ? null : t.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static List<Path> resolveAllAtomFilesSameDir(Path root) {
        Path dir = root.getParent();
        if (dir == null) return List.of(root);

        List<Path> all;
        try (Stream<Path> s = Files.list(dir)) {
            all = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".atom"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of(root);
        }

        LinkedHashSet<Path> ordered = new LinkedHashSet<>();
        ordered.add(root);
        ordered.addAll(all);
        return new ArrayList<>(ordered);
    }
}

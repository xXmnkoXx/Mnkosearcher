package com.ordenatec.portallicitaciones.infra.importacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.ordenatec.portallicitaciones.domain.enums.EstadoLicitacion;
import com.ordenatec.portallicitaciones.domain.enums.TipoContrato;
import com.ordenatec.portallicitaciones.domain.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Acumulador para Cataluña (Socrata ybgg-dgi6):
 * - El dataset suele venir "por lote" y/o por fase, por lo que hay múltiples filas
 *   con el MISMO codi_expedient.
 * - Tu upsert actual hace clearLotes/clearDocumentos, así que hay que agrupar antes.
 */
public final class CatalunyaLicitacionAccumulator {

    // Ajusta esto al tamaño real de tu columna si lo sabes (ej: 15, 20, 50...)
    private static final int MAX_ADJ_NIF_LEN = 32;
    private static final int MAX_ADJ_NAME_LEN = 255;

    private CatalunyaLicitacionAccumulator() {}

    public static void addRow(Map<String, Licitacion> byExpediente, JsonNode row) {
        if (byExpediente == null) throw new IllegalArgumentException("byExpediente null");
        if (row == null || row.isNull()) return;

        String expediente = CatalunyaJsonExtractors.expediente(row);
        if (expediente == null || expediente.isBlank()) return;

        expediente = normalizeExpediente(expediente);

        Licitacion lic = byExpediente.computeIfAbsent(expediente, k -> {
            Licitacion n = new Licitacion();
            n.setId(UUID.randomUUID());
            n.setExpediente(k);
            n.setLastEventUpdatedAt(Instant.now());
            return n;
        });

        // ---------- CORE ----------
        if (isBlank(lic.getTitulo())) {
            lic.setTitulo(CatalunyaJsonExtractors.titulo(row));
        }

        if (isBlank(lic.getUrlPublica())) {
            lic.setUrlPublica(CatalunyaJsonExtractors.urlPublica(row));
        }

        if (lic.getTipoContrato() == null) {
            lic.setTipoContrato(mapTipoContrato(CatalunyaJsonExtractors.tipusContracteRaw(row)));
        }
        if (isBlank(lic.getProcedimiento())) {
            lic.setProcedimiento(CatalunyaJsonExtractors.procediment(row));
        }
        if (isBlank(lic.getTramitacion())) {
            lic.setTramitacion(CatalunyaJsonExtractors.tramitacio(row));
        }

        if (lic.getEstado() == null || lic.getEstado() == EstadoLicitacion.DESCONOCIDA) {
            lic.setEstado(mapEstado(CatalunyaJsonExtractors.fasePublicacio(row), CatalunyaJsonExtractors.resultat(row)));
        }

        // Fechas
        if (lic.getFechas() == null) {
            lic.setFechas(new FechasProcedimiento());
        }
        FechasProcedimiento fechas = lic.getFechas();

        LocalDate pub = CatalunyaJsonExtractors.fechaPublicacionBestEffort(row);
        if (fechas.getFechaPublicacion() == null && pub != null) {
            fechas.setFechaPublicacion(pub);
        } else if (fechas.getFechaPublicacion() != null && pub != null) {
            if (pub.isAfter(fechas.getFechaPublicacion())) {
                fechas.setFechaPublicacion(pub);
            }
        }

        LocalDate limite = CatalunyaJsonExtractors.fechaLimitePresentacion(row);
        if (fechas.getFechaLimitePresentacion() == null && limite != null) {
            fechas.setFechaLimitePresentacion(limite);
        }

        // Presupuesto / valor estimado
        if (lic.getPresupuestoBase() == null) {
            var amount = CatalunyaJsonExtractors.presupuestoBaseAmount(row);
            var incIva = CatalunyaJsonExtractors.presupuestoIncluyeIVA(row);
            if (amount != null) {
                Money m = new Money();
                m.setAmount(amount);
                m.setCurrency("EUR");
                m.setIncluyeIVA(incIva);
                lic.setPresupuestoBase(m);
            }
        }
        if (lic.getValorEstimado() == null) {
            var ve = CatalunyaJsonExtractors.valorEstimadoAmount(row);
            if (ve != null) {
                Money m = new Money();
                m.setAmount(ve);
                m.setCurrency("EUR");
                lic.setValorEstimado(m);
            }
        }

        // Organismo
        if (lic.getOrganismo() == null) {
            OrganismoContratacion org = new OrganismoContratacion();
            org.setNombre(CatalunyaJsonExtractors.organismoNombre(row));
            org.setDir3(CatalunyaJsonExtractors.organismoDir3(row));
            org.setTipoAdministracion(CatalunyaJsonExtractors.organismoTipoAdministracion(row));
            lic.setOrganismo(org);
        } else {
            if (isBlank(lic.getOrganismo().getNombre())) {
                lic.getOrganismo().setNombre(CatalunyaJsonExtractors.organismoNombre(row));
            }
            if (isBlank(lic.getOrganismo().getDir3())) {
                lic.getOrganismo().setDir3(CatalunyaJsonExtractors.organismoDir3(row));
            }
        }

        // CPVs (UNION)
        Set<String> cpvCodes = CatalunyaJsonExtractors.extractCpvCodes(row);
        if (cpvCodes != null && !cpvCodes.isEmpty()) {
            Set<String> existing = new HashSet<>();
            for (Cpv c : safeList(lic.getCpvs())) {
                if (c != null && c.getCodigo() != null) existing.add(c.getCodigo());
            }
            for (String code : cpvCodes) {
                if (code == null || code.isBlank()) continue;
                if (existing.contains(code)) continue;

                Cpv cpv = new Cpv();
                cpv.setCodigo(code);
                lic.getCpvs().add(cpv);
                existing.add(code);
            }
        }

        // LOTES (UNION)
        String numLote = CatalunyaJsonExtractors.numeroLote(row);
        String descLote = CatalunyaJsonExtractors.descripcionLote(row);

        if (!isBlank(numLote) || !isBlank(descLote)) {
            String loteKey = normalizeLoteKey(numLote, descLote);

            Set<String> existingLotes = new HashSet<>();
            for (Lote l : safeList(lic.getLotes())) {
                if (l == null) continue;
                existingLotes.add(normalizeLoteKey(l.getNumero(), l.getTitulo()));
            }

            if (!existingLotes.contains(loteKey)) {
                Lote lote = new Lote();
                lote.setNumero(numLote);
                lote.setTitulo(descLote);

                if (lic.getCpvs() != null && !lic.getCpvs().isEmpty() && lic.getCpvs().get(0) != null) {
                    lote.setCpvs(List.of(lic.getCpvs().get(0)));
                }

                lic.getLotes().add(lote);
            }
        }

        // ADJUDICACIÓN (best-effort)
        boolean hasAdjData =
                !isBlank(CatalunyaJsonExtractors.adjudicatarioNombre(row)) ||
                !isBlank(CatalunyaJsonExtractors.adjudicatarioNif(row)) ||
                CatalunyaJsonExtractors.importeAdjudicacionAmbIva(row) != null ||
                CatalunyaJsonExtractors.importeAdjudicacionSenseIva(row) != null;

        if (hasAdjData) {
            Adjudicacion a = lic.getAdjudicacion();
            if (a == null) {
                a = new Adjudicacion();
                lic.setAdjudicacion(a);
            }

            if (isBlank(a.getAdjudicatarioNombre())) {
                String nombre = normalizeText(CatalunyaJsonExtractors.adjudicatarioNombre(row));
                a.setAdjudicatarioNombre(truncate(nombre, MAX_ADJ_NAME_LEN));
            }

            if (isBlank(a.getAdjudicatarioNif())) {
                String nifRaw = normalizeText(CatalunyaJsonExtractors.adjudicatarioNif(row));
                String nif = firstToken(nifRaw); // 👈 evita "B605...||A283..."
                a.setAdjudicatarioNif(truncate(nif, MAX_ADJ_NIF_LEN));
            }

            Integer ofertas = CatalunyaJsonExtractors.ofertesRebudes(row);
            if (a.getNumeroOfertas() == null && ofertas != null) {
                a.setNumeroOfertas(ofertas);
            }

            // Importe adjudicado (preferimos con IVA)
            if (a.getImporteAdjudicado() == null) {
                var amb = CatalunyaJsonExtractors.importeAdjudicacionAmbIva(row);
                var sense = CatalunyaJsonExtractors.importeAdjudicacionSenseIva(row);

                if (amb != null || sense != null) {
                    Money m = new Money();
                    m.setCurrency("EUR");
                    m.setIncluyeIVA(amb != null);
                    m.setAmount(amb != null ? amb : sense);
                    a.setImporteAdjudicado(m);
                }
            }
        }

        lic.setLastEventUpdatedAt(Instant.now());
    }

    // =============================
    // MAPEOS
    // =============================

    private static TipoContrato mapTipoContrato(String raw) {
        if (raw == null) return TipoContrato.OTRO;
        String s = raw.trim().toLowerCase(Locale.ROOT);

        if (s.contains("obra")) return TipoContrato.OBRAS;
        if (s.contains("submin") || s.contains("sumin")) return TipoContrato.SUMINISTROS;
        if (s.contains("servei") || s.contains("servicio") || s.contains("serv")) return TipoContrato.SERVICIOS;
        if (s.contains("concess") || s.contains("conces")) return TipoContrato.CONCESION;
        if (s.contains("mixt")) return TipoContrato.MIXTO;

        return TipoContrato.OTRO;
    }

    private static EstadoLicitacion mapEstado(String fasePublicacio, String resultat) {
        String fase = (fasePublicacio == null ? "" : fasePublicacio).toLowerCase(Locale.ROOT);
        String res = (resultat == null ? "" : resultat).toLowerCase(Locale.ROOT);

        if (fase.contains("anul") || res.contains("anul")) return EstadoLicitacion.ANULADA;
        if (res.contains("desert") || fase.contains("desert")) return EstadoLicitacion.DESIERTA;
        if (fase.contains("adjud") || res.contains("adjud")) return EstadoLicitacion.ADJUDICADA;
        if (fase.contains("avalu") || fase.contains("evalu")) return EstadoLicitacion.EN_EVALUACION;

        if (!fase.isBlank()) return EstadoLicitacion.PUBLICADA;
        return EstadoLicitacion.DESCONOCIDA;
    }

    // =============================
    // HELPERS
    // =============================

    private static String normalizeExpediente(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ').trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeLoteKey(String numero, String titulo) {
        String n = (numero == null ? "" : numero).trim();
        String t = (titulo == null ? "" : titulo).trim();
        return (n + "|" + t).toLowerCase(Locale.ROOT);
    }

    private static String normalizeText(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ').trim();
        return t.isBlank() ? null : t;
    }

    private static String firstToken(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;

        int idx = t.indexOf("||");
        if (idx > 0) t = t.substring(0, idx);

        idx = t.indexOf(",");
        if (idx > 0) t = t.substring(0, idx);

        return t.trim();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return (s.length() <= maxLen) ? s : s.substring(0, maxLen);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> List<T> safeList(List<T> list) {
        return (list == null) ? List.of() : list;
    }
}

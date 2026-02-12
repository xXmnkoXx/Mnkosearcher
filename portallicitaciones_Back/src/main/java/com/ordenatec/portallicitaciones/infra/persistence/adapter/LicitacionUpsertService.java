package com.ordenatec.portallicitaciones.infra.persistence.adapter;

import com.ordenatec.portallicitaciones.domain.model.*;
import com.ordenatec.portallicitaciones.infra.persistence.entity.*;
import com.ordenatec.portallicitaciones.infra.persistence.repository.CpvJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.OrganismoJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LicitacionUpsertService {

    private final LicitacionJpaRepository licitacionJpa;
    private final CpvJpaRepository cpvJpa;
    private final OrganismoJpaRepository organismoJpa;

    public LicitacionUpsertService(
            LicitacionJpaRepository licitacionJpa,
            CpvJpaRepository cpvJpa,
            OrganismoJpaRepository organismoJpa
    ) {
        this.licitacionJpa = licitacionJpa;
        this.cpvJpa = cpvJpa;
        this.organismoJpa = organismoJpa;
    }

    @Transactional
    public LicitacionEntity upsert(Licitacion lic) {
        if (lic == null) throw new IllegalArgumentException("licitacion null");

        // ✅ Normaliza expediente (evita "duplicados" invisibles: NBSP, tabs, etc.)
        String expediente = normalizeExpediente(lic.getExpediente());
        if (expediente == null) {
            throw new IllegalArgumentException("La licitación no trae expediente (ContractFolderID). No se puede hacer upsert.");
        }

        try {
            LicitacionEntity entity = licitacionJpa.findByExpediente(expediente)
                    .orElseGet(LicitacionEntity::new);

            mapToEntity(lic, expediente, entity);

            return licitacionJpa.save(entity);

        } catch (DataIntegrityViolationException ex) {
            LicitacionEntity existing = licitacionJpa.findByExpediente(expediente)
                    .orElseThrow(() -> ex);

            mapToEntity(lic, expediente, existing);

            return licitacionJpa.save(existing);
        }
    }

    // ----------------- MAPEOS -----------------

    private void mapToEntity(Licitacion lic, String expediente, LicitacionEntity entity) {

        // 1) Campos simples
        if (entity.getUuid() == null) {
            entity.setUuid(lic.getId() != null ? lic.getId() : UUID.randomUUID());
        }

        entity.setExpediente(expediente);
        entity.setTitulo(nullToEmpty(lic.getTitulo()));
        entity.setUrlPublica(lic.getUrlPublica());
        entity.setFechaUltimaActualizacion(lic.getLastEventUpdatedAt());

        // 1b) ✅ NUEVO: campos Navarra/similares (columna directa)
        entity.setEntidad(safe(lic.getEntidad()));

        // Textos (para poder comparar y filtrar aunque luego uses enums)
        entity.setTipoContratoTexto(lic.getTipoContrato() != null ? lic.getTipoContrato().name() : null);
        entity.setModalidad(safe(lic.getTramitacion()));         // Navarra: "Modalidad" -> tramitación (texto)
        entity.setProcedimiento(safe(lic.getProcedimiento()));
        entity.setCriterioAdjudicacion(safe(lic.getCriterioAdjudicacion()));

        entity.setCodigoNuts(safe(lic.getCodigoNuts()));
        entity.setLugarEjecucion(safe(lic.getLugarEjecucion()));

        entity.setEstadoTexto(lic.getEstado() != null ? lic.getEstado().name() : null);
        entity.setMotivo(safe(lic.getMotivo()));
        entity.setPlazoEjecucion(safe(lic.getPlazoEjecucion()));

        // ==========================
        // ✅ FECHAS (AQUÍ ESTABA LO QUE TE FALTA)
        // ==========================
        LocalDate fechaPub = null;
        LocalDate fechaLimite = null;

        if (lic.getFechas() != null) {
            fechaPub = lic.getFechas().getFechaPublicacion();

            // ✅ IMPORTANTE: usa TU getter real
            // En tus objetos JSON sale como "fechaLimitePresentacion"
            // así que este getter debería existir:
            fechaLimite = lic.getFechas().getFechaLimitePresentacion();

            // Si tu clase lo llama distinto, cambia la línea de arriba por el getter correcto.
        }

        entity.setFechaPublicacion(fechaPub);

        // ✅ NO machacamos con null (si ya tenías una guardada)
        if (fechaLimite != null) {
            entity.setFechaLimitePresentacion(fechaLimite);
        }

        // Importes
        BigDecimal precio = null;
        if (lic.getPresupuestoBase() != null) {
            precio = lic.getPresupuestoBase().getAmount();
        }
        entity.setPrecioLicitacion(precio);

        BigDecimal ve = null;
        if (lic.getValorEstimado() != null) {
            ve = lic.getValorEstimado().getAmount();
        }
        entity.setValorEstimado(ve);

        // Moneda
        String moneda = safe(lic.getMoneda());
        if (moneda == null) {
            if (lic.getPresupuestoBase() != null && safe(lic.getPresupuestoBase().getCurrency()) != null) {
                moneda = lic.getPresupuestoBase().getCurrency();
            } else if (lic.getValorEstimado() != null && safe(lic.getValorEstimado().getCurrency()) != null) {
                moneda = lic.getValorEstimado().getCurrency();
            }
        }
        entity.setMoneda(moneda);

        // 2) Organismo
        if (lic.getOrganismo() != null) {
            OrganismoEntity org = resolveOrganismo(lic.getOrganismo());
            entity.setOrganismo(org);
        }

        // 3) CPVs
        entity.getCpvs().clear();
        if (lic.getCpvs() != null) {
            for (Cpv c : lic.getCpvs()) {
                if (c == null) continue;
                String codigo = safe(c.getCodigo());
                if (codigo == null) continue;

                CpvEntity cpv = cpvJpa.findByCodigo(codigo)
                        .orElseGet(() -> {
                            CpvEntity n = new CpvEntity();
                            n.setCodigo(codigo);
                            return n;
                        });

                cpvJpa.save(cpv);
                entity.getCpvs().add(cpv);
            }
        }

        // 4) Documentos
        entity.clearDocumentos();
        if (lic.getDocumentos() != null) {
            for (Documento d : lic.getDocumentos()) {
                if (d == null) continue;
                if (safe(d.getUrl()) == null && safe(d.getTitulo()) == null) continue;

                DocumentoEntity de = new DocumentoEntity();
                de.setTipo(d.getTipo());
                de.setTitulo(d.getTitulo());
                de.setUrl(d.getUrl());
                entity.addDocumento(de);
            }
        }

        // 5) Lotes
        entity.clearLotes();
        if (lic.getLotes() != null) {
            for (Lote l : lic.getLotes()) {
                if (l == null) continue;

                LoteEntity le = new LoteEntity();
                le.setNumero(parseIntSafe(l.getNumero()));
                le.setTitulo(l.getTitulo());

                if (l.getCpvs() != null && !l.getCpvs().isEmpty() && l.getCpvs().get(0) != null) {
                    le.setCpvPrincipal(l.getCpvs().get(0).getCodigo());
                }

                entity.addLote(le);
            }
        }

        // 6) Adjudicación (FK)
        if (lic.getAdjudicacion() != null) {
            AdjudicacionEntity ae = (entity.getAdjudicacion() != null)
                    ? entity.getAdjudicacion()
                    : new AdjudicacionEntity();

            mapAdjudicacion(lic.getAdjudicacion(), ae);

            ae.setLicitacion(entity);
            entity.setAdjudicacion(ae);
        }
    }

    // ----------------- helpers -----------------

    private OrganismoEntity resolveOrganismo(OrganismoContratacion o) {

        final String key = firstNonBlank(
                safe(o.getDir3()),
                safe(o.getNif()),
                safe(o.getNombre())
        );

        OrganismoEntity entity;
        if (key != null) {
            entity = organismoJpa.findByCodigo(key).orElseGet(() -> {
                OrganismoEntity n = new OrganismoEntity();
                n.setCodigo(key);
                return n;
            });
        } else {
            entity = new OrganismoEntity();
            entity.setCodigo(UUID.randomUUID().toString());
        }

        entity.setNombre(o.getNombre());
        entity.setDir3(o.getDir3());
        entity.setNif(o.getNif());

        return organismoJpa.save(entity);
    }

    private void mapAdjudicacion(Adjudicacion a, AdjudicacionEntity e) {
        e.setAdjudicatarioNombre(a.getAdjudicatarioNombre());
        e.setAdjudicatarioNif(a.getAdjudicatarioNif());
        e.setNumeroOfertas(a.getNumeroOfertas());
        e.setCriterio(a.getCriterio());

        if (a.getImporteAdjudicado() != null) {
            if (a.getImporteAdjudicado().getAmount() != null) {
                e.setImporteAdjudicado(a.getImporteAdjudicado().getAmount());
            }
            if (a.getImporteAdjudicado().getCurrency() != null) {
                e.setMoneda(a.getImporteAdjudicado().getCurrency());
            }
        }
    }

    private static String normalizeExpediente(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ').trim();
        if (t.isEmpty()) return null;
        return t;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static Integer parseIntSafe(String s) {
        if (s == null) return null;
        String t = s.replaceAll("\\D+", "").trim();
        if (t.isEmpty()) return null;
        try { return Integer.parseInt(t); } catch (Exception ignore) { return null; }
    }

    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

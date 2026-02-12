package com.ordenatec.portallicitaciones.infra.persistence.adapter;

import com.ordenatec.portallicitaciones.domain.enums.EstadoLicitacion;
import com.ordenatec.portallicitaciones.domain.enums.TipoContrato;
import com.ordenatec.portallicitaciones.domain.model.FechasProcedimiento;
import com.ordenatec.portallicitaciones.domain.model.Licitacion;
import com.ordenatec.portallicitaciones.domain.model.Money;
import com.ordenatec.portallicitaciones.domain.model.OrganismoContratacion;
import com.ordenatec.portallicitaciones.domain.port.LicitacionRepository;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.OrganismoEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionJpaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class LicitacionRepositorySqlServer implements LicitacionRepository {

    private final LicitacionJpaRepository jpaRepository;

    public LicitacionRepositorySqlServer(LicitacionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Licitacion save(Licitacion licitacion) {
        if (licitacion == null) throw new IllegalArgumentException("licitacion null");

        String expediente = safe(licitacion.getExpediente());
        if (expediente == null) {
            throw new IllegalArgumentException("Licitación sin expediente");
        }

        // ✅ Upsert por EXPEDIENTE (estable)
        LicitacionEntity entity = jpaRepository.findByExpediente(expediente)
                .orElseGet(() -> (licitacion.getId() != null
                        ? jpaRepository.findByUuid(licitacion.getId()).orElseGet(LicitacionEntity::new)
                        : new LicitacionEntity()));

        if (entity.getUuid() == null) {
            entity.setUuid(licitacion.getId() != null ? licitacion.getId() : UUID.randomUUID());
        }

        // Base
        entity.setExpediente(expediente);
        entity.setTitulo(licitacion.getTitulo());
        entity.setUrlPublica(licitacion.getUrlPublica());
        entity.setFechaUltimaActualizacion(licitacion.getLastEventUpdatedAt());

        // Textos
        entity.setEntidad(safe(licitacion.getEntidad()));
        entity.setProcedimiento(safe(licitacion.getProcedimiento()));
        entity.setCriterioAdjudicacion(safe(licitacion.getCriterioAdjudicacion()));
        entity.setCodigoNuts(safe(licitacion.getCodigoNuts()));
        entity.setLugarEjecucion(safe(licitacion.getLugarEjecucion()));
        entity.setMotivo(safe(licitacion.getMotivo()));
        entity.setPlazoEjecucion(safe(licitacion.getPlazoEjecucion()));

        // Moneda
        entity.setMoneda(safe(licitacion.getMoneda()));

        // ✅ ORGANISMO:
        // NO lo tocamos aquí porque en Domain OrganismoContratacion NO trae id/código.
        // El organismo_id debe venir ya resuelto en tu proceso de ingesta/enriquecimiento.
        // (Si quisieras resolverlo aquí, necesitarías un OrganismoJpaRepository y buscar por dir3/nif.)

        // Estado / TipoContrato: guardados como TEXTO en entity
        if (licitacion.getEstado() != null) {
            entity.setEstadoTexto(licitacion.getEstado().name());
        }
        if (licitacion.getTipoContrato() != null) {
            entity.setTipoContratoTexto(licitacion.getTipoContrato().name());
        }

        // ==========================
        // FECHAS
        // ==========================
        if (licitacion.getFechas() != null) {
            if (licitacion.getFechas().getFechaPublicacion() != null) {
                entity.setFechaPublicacion(licitacion.getFechas().getFechaPublicacion());
            }
            if (licitacion.getFechas().getFechaLimitePresentacion() != null) {
                entity.setFechaLimitePresentacion(licitacion.getFechas().getFechaLimitePresentacion());
            }
        }

        // Importes
        if (licitacion.getPresupuestoBase() != null) {
            entity.setPrecioLicitacion(toBigDecimal(licitacion.getPresupuestoBase()));
        }
        if (licitacion.getValorEstimado() != null) {
            entity.setValorEstimado(toBigDecimal(licitacion.getValorEstimado()));
        }

        // Moneda fallback
        if (entity.getMoneda() == null || entity.getMoneda().isBlank()) {
            String cur = null;
            if (licitacion.getPresupuestoBase() != null) cur = safe(licitacion.getPresupuestoBase().getCurrency());
            if (cur == null && licitacion.getValorEstimado() != null) cur = safe(licitacion.getValorEstimado().getCurrency());
            if (cur != null) entity.setMoneda(cur);
        }

        jpaRepository.save(entity);
        return licitacion;
    }

    @Override
    public Optional<Licitacion> findById(UUID id) {
        // ✅ Recomendado: usar un método con EntityGraph que traiga organismo
        // si no, puede petar al serializar por LAZY fuera de sesión.
        return jpaRepository.findByUuid(id).map(this::toDomain);
    }

    @Override
    public Optional<Licitacion> findByUrlPublica(String urlPublica) {
        if (urlPublica == null || urlPublica.isBlank()) return Optional.empty();
        return jpaRepository.findByUrlPublica(urlPublica.trim()).map(this::toDomain);
    }

    @Override
    public Optional<Licitacion> findByExpediente(String expediente) {
        if (expediente == null || expediente.isBlank()) return Optional.empty();
        return jpaRepository.findByExpediente(expediente.trim()).map(this::toDomain);
    }

    @Override
    public List<Licitacion> findAll() {
        // ✅ MUY recomendado: crea en LicitacionJpaRepository un método con @EntityGraph(attributePaths={"organismo"})
        // y úsalo aquí para evitar LazyInitializationException:
        //
        // return jpaRepository.findAllWithOrganismo()
        //         .stream().map(this::toDomain).toList();

        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Licitacion toDomain(LicitacionEntity e) {
        Licitacion l = new Licitacion();

        l.setId(e.getUuid());
        l.setExpediente(e.getExpediente());
        l.setTitulo(e.getTitulo());
        l.setUrlPublica(e.getUrlPublica());
        l.setLastEventUpdatedAt(e.getFechaUltimaActualizacion());

        // Textos
        l.setEntidad(e.getEntidad());
        l.setProcedimiento(e.getProcedimiento());
        l.setCriterioAdjudicacion(e.getCriterioAdjudicacion());
        l.setCodigoNuts(e.getCodigoNuts());
        l.setLugarEjecucion(e.getLugarEjecucion());
        l.setMotivo(e.getMotivo());
        l.setPlazoEjecucion(e.getPlazoEjecucion());
        l.setMoneda(e.getMoneda());

        // ✅ ORGANISMO: aquí es donde “sale el nombre” para el front
        OrganismoEntity oe = e.getOrganismo();
        if (oe != null) {
            OrganismoContratacion org = new OrganismoContratacion();
            org.setNombre(oe.getNombre());
            org.setDir3(oe.getDir3());
            org.setNif(oe.getNif());

            // Si tu OrganismoEntity tiene tipoAdministracion, descomenta y ajusta getter:
            // org.setTipoAdministracion(oe.getTipoAdministracion());

            l.setOrganismo(org);
        } else {
            l.setOrganismo(null);
        }

        // Estado / TipoContrato desde texto
        l.setEstado(parseEstado(e.getEstadoTexto()));
        l.setTipoContrato(parseTipoContrato(e.getTipoContratoTexto()));

        // ==========================
        // FECHAS
        // ==========================
        if (e.getFechaPublicacion() != null || e.getFechaLimitePresentacion() != null) {
            FechasProcedimiento f = new FechasProcedimiento();
            f.setFechaPublicacion(e.getFechaPublicacion());
            f.setFechaLimitePresentacion(e.getFechaLimitePresentacion());
            l.setFechas(f);
        } else {
            l.setFechas(null);
        }

        // Importes
        if (e.getPrecioLicitacion() != null) {
            l.setPresupuestoBase(toMoney(e.getPrecioLicitacion(), e.getMoneda()));
        } else {
            l.setPresupuestoBase(null);
        }

        if (e.getValorEstimado() != null) {
            l.setValorEstimado(toMoney(e.getValorEstimado(), e.getMoneda()));
        } else {
            l.setValorEstimado(null);
        }

        return l;
    }

    @Override
    public Optional<UUID> findIdByExpediente(String expediente) {
        String exp = safe(expediente);
        if (exp == null) return Optional.empty();
        return jpaRepository.findByExpediente(exp).map(LicitacionEntity::getUuid);
    }

    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static BigDecimal toBigDecimal(Money money) {
        if (money == null) return null;
        return money.getAmount();
    }

    private static Money toMoney(BigDecimal amount, String currency) {
        if (amount == null) return null;
        Money m = new Money();
        m.setAmount(amount);
        m.setCurrency((currency == null || currency.isBlank()) ? "EUR" : currency);
        return m;
    }

    private static EstadoLicitacion parseEstado(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toUpperCase();
        try {
            return EstadoLicitacion.valueOf(s);
        } catch (Exception ignore) {
            if (s.contains("PUBLIC")) return EstadoLicitacion.PUBLICADA;
            if (s.contains("ADJUD")) return EstadoLicitacion.ADJUDICADA;
            if (s.contains("CANCEL")) return EstadoLicitacion.CANCELADA;
            if (s.contains("ANUL")) return EstadoLicitacion.ANULADA;
            return EstadoLicitacion.DESCONOCIDA;
        }
    }

    private static TipoContrato parseTipoContrato(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toUpperCase();
        try {
            return TipoContrato.valueOf(s);
        } catch (Exception ignore) {
            if (s.contains("OBRA")) return TipoContrato.OBRAS;
            if (s.contains("SUMIN")) return TipoContrato.SUMINISTROS;
            if (s.contains("SERV")) return TipoContrato.SERVICIOS;
            if (s.contains("MIXT")) return TipoContrato.MIXTO;
            return TipoContrato.OTRO;
        }
    }
}

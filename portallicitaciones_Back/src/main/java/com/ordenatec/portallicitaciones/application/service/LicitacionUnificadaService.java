package com.ordenatec.portallicitaciones.application.service;

import com.ordenatec.portallicitaciones.api.dto.LicitacionUnificadaDto;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionCatalunyaEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionNacionalEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionCatalunyaJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.LicitacionNacionalJpaRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LicitacionUnificadaService {

    private final LicitacionNacionalJpaRepository nacionalRepo;
    private final LicitacionCatalunyaJpaRepository catalunyaRepo;

    public LicitacionUnificadaService(LicitacionNacionalJpaRepository nacionalRepo,
                                      LicitacionCatalunyaJpaRepository catalunyaRepo) {
        this.nacionalRepo = nacionalRepo;
        this.catalunyaRepo = catalunyaRepo;
    }

    public Page<LicitacionUnificadaDto> listarUnificadas(String source, String q, String estado, int page, int size) {

        // source: mix | nacional | catalunya
        String src = (source == null || source.isBlank()) ? "mix" : source.trim().toLowerCase();

        // estado: all | en_plazo | vencidas
        String est = (estado == null || estado.isBlank()) ? "all" : estado.trim().toLowerCase();

        String query = (q == null) ? "" : q.trim();

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 500);

        // Sort (la entidad Nacional tiene fechaPublicacion)
        Sort sortNacional = Sort.by(Sort.Direction.DESC, "fechaPublicacion");

        // 1) NACIONAL con filtro de estado -> PAGINACIÓN REAL (esto es lo que necesitas)
        // Además: si source=mix pero estado != all, devolvemos SOLO nacional (hasta clasificar catalunya)
        if (src.equals("nacional") || (src.equals("mix") && !est.equals("all"))) {

            Pageable p = PageRequest.of(safePage, safeSize, sortNacional);

            Page<LicitacionNacionalEntity> nPage;
            switch (est) {
                case "en_plazo" -> nPage = nacionalRepo.searchEnPlazo(query, p);
                case "vencidas" -> nPage = nacionalRepo.searchVencidas(query, p);
                case "all" -> nPage = nacionalRepo.searchAll(query, p);
                default -> nPage = nacionalRepo.searchAll(query, p);
            }

            return nPage.map(this::mapNacional);
        }

        // 2) Solo CATALUNYA (sin filtro de estado por ahora)
        if (src.equals("catalunya")) {
            Pageable pc = PageRequest.of(
                    safePage,
                    safeSize,
                    Sort.by(Sort.Direction.DESC, "dataPublicacioAnunci")
            );

            Page<LicitacionCatalunyaEntity> cPage = query.isEmpty()
                    ? catalunyaRepo.findAll(pc)
                    : catalunyaRepo.findByCodiExpedientContainingIgnoreCaseOrDenominacioContainingIgnoreCaseOrNomOrganContainingIgnoreCase(
                            query, query, query, pc
                    );

            return cPage.map(this::mapCatalunya);
        }

        // 3) MIX + estado=all (tu comportamiento actual: mezcla in-memory)
        //    Nota: aquí el totalElements NO es el real global, es "items.size()" de lo que hemos traído.
        //    Pero como esto es solo para "all", lo aceptamos de momento.
        int need = (safePage + 1) * safeSize;

        Pageable p0N = PageRequest.of(0, need, sortNacional);
        Pageable p0C = PageRequest.of(0, need, Sort.by(Sort.Direction.DESC, "dataPublicacioAnunci"));

        List<LicitacionUnificadaDto> items = new ArrayList<>();

        Page<LicitacionNacionalEntity> nPage = query.isEmpty()
                ? nacionalRepo.searchAll(query, p0N)
                : nacionalRepo.searchAll(query, p0N); // searchAll ya contempla q vacío/nulo

        for (LicitacionNacionalEntity n : nPage.getContent()) {
            items.add(mapNacional(n));
        }

        Page<LicitacionCatalunyaEntity> cPage = query.isEmpty()
                ? catalunyaRepo.findAll(p0C)
                : catalunyaRepo.findByCodiExpedientContainingIgnoreCaseOrDenominacioContainingIgnoreCaseOrNomOrganContainingIgnoreCase(
                        query, query, query, p0C
                );

        for (LicitacionCatalunyaEntity c : cPage.getContent()) {
            items.add(mapCatalunya(c));
        }

        items.sort(Comparator.comparing(
                LicitacionUnificadaDto::fechaPublicacion,
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());

        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, items.size());
        List<LicitacionUnificadaDto> slice = from >= items.size() ? List.of() : items.subList(from, to);

        return new PageImpl<>(slice, PageRequest.of(safePage, safeSize), items.size());
    }

    private LicitacionUnificadaDto mapNacional(LicitacionNacionalEntity n) {
        String dur = null;
        if (n.getDuracion() != null) {
            dur = n.getDuracion().toPlainString();
            if (n.getDuracionUnidad() != null && !n.getDuracionUnidad().isBlank()) {
                dur = dur + " " + n.getDuracionUnidad();
            }
        }

        return new LicitacionUnificadaDto(
                "NACIONAL",
                (n.getId() != null ? n.getId() : n.getUrl()),
                n.getExpediente(),
                n.getObjeto(),
                n.getObjeto(),
                n.getOrganoContratante(),
                n.getDir3Organo(),
                n.getDependencia(),
                n.getTipoContrato(),
                n.getProcedimiento(),
                n.getEstado(), // <- esto es lo que tú ves como estadoFase en el JSON
                n.getCpvPrincipal(),
                n.getCpvs(),
                n.getNuts(),
                n.getUbicacion(),
                n.getImporteSinIva(),
                n.getImporteSinIva(),
                n.getImporteConIva(),
                n.getImporteAdjudicacion(),
                n.getImporteAdjConIva(),
                n.getAdjudicatario(),
                n.getNifAdjudicatario(),
                n.getNumOfertas(),
                n.getFechaPublicacion(),
                n.getFechaLimite(),
                n.getFechaAdjudicacion(),
                dur,
                n.getUrl()
        );
    }

    private LicitacionUnificadaDto mapCatalunya(LicitacionCatalunyaEntity c) {
        return new LicitacionUnificadaDto(
                "CATALUNYA",
                c.getEnllacPublicacio(),
                c.getCodiExpedient(),
                c.getDenominacio(),
                c.getObjecteContracte(),
                c.getNomOrgan(),
                c.getCodiDir3(),
                c.getNomDepartamentEns(),
                c.getTipusContracte(),
                c.getProcediment(),
                c.getFasePublicacio(),
                c.getCodiCpv(),
                c.getCodiCpv(),
                c.getCodiNuts(),
                c.getLlocExecucio(),
                c.getValorEstimatContracte(),
                c.getPressupostLicitacioSenseIva(),
                c.getPressupostLicitacioAmbIva(),
                c.getImportAdjudicacioSenseIva(),
                c.getImportAdjudicacioAmbIva(),
                c.getDenominacioAdjudicatari(),
                c.getIdentificacioAdjudicatari(),
                c.getOfertesRebudes(),
                c.getDataPublicacioAnunci(),
                c.getTerminiPresentacioOfertes(),
                c.getDataAdjudicacioContracte(),
                c.getDuradaContracte(),
                c.getEnllacPublicacio()
        );
    }
}

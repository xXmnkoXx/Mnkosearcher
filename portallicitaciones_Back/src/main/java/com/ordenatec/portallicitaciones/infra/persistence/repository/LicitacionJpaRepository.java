package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LicitacionJpaRepository extends JpaRepository<LicitacionEntity, Long> {

    Optional<LicitacionEntity> findByUuid(UUID uuid);

    /**
     * ✅ Detalle “FULL” base: carga relaciones que NO provocan MultipleBagFetchException.
     * Documentos y lotes se inicializan en el Service dentro de la transacción.
     */
    @EntityGraph(attributePaths = {
            "organismo",
            "cpvs",
            "adjudicacion"
    })
    Optional<LicitacionEntity> findWithAllByUuid(UUID uuid);

    Optional<LicitacionEntity> findByExpediente(String expediente);

    Optional<LicitacionEntity> findByUrlPublica(String urlPublica);

    // ✅ Para "Mis licitaciones enviadas": mapear expediente/id_externo -> uuid
    // (Spring Data genera la query automáticamente.)
    List<LicitacionEntity> findAllByExpedienteIn(List<String> expedientes);

    /**
     * ✅ LISTADO PARA BUSCADOR (GET /api/licitaciones)
     * Trae organismo + cpvs para que el front muestre organismo.nombre (y nif si lo necesitas).
     * OJO: NO trae documentos/lotes (evitamos fetch pesado).
     */
    @EntityGraph(attributePaths = { "organismo", "cpvs" })
    @Query("""
        select l
        from LicitacionEntity l
        order by l.fechaPublicacion desc nulls last, l.id desc
    """)
    List<LicitacionEntity> findAllForList();

    /**
     * Lista de IDs de licitaciones de Euskadi a las que les falta estado (o force=true).
     * Filtra por urlPublica que contenga el dominio de Euskadi y por ventana de fechaPublicacion.
     *
     * hostPattern típico: "contratacion.euskadi.eus"
     * fromDate: LocalDate.now().minusDays(daysBack)
     */
    @Query("""
        select l.id
        from LicitacionEntity l
        where l.urlPublica is not null
          and lower(l.urlPublica) like concat('%', lower(:hostPattern), '%')
          and (:force = true or l.estadoTexto is null or trim(l.estadoTexto) = '')
          and (l.fechaPublicacion is null or l.fechaPublicacion >= :fromDate)
        order by l.fechaPublicacion desc nulls last, l.id desc
    """)
    List<Long> findIdsEuskadiPendientesEstado(
            @Param("hostPattern") String hostPattern,
            @Param("fromDate") LocalDate fromDate,
            @Param("force") boolean force,
            Pageable pageable
    );

    /**
     * ✅ LISTADO PUBLICO PARA N8N / BUSCADOR
     * - estado opcional
     * - fechaLimitePresentacion >= fromDate (si se pasa)
     * - filtro cpvs opcional (csv -> List en service)
     *
     * IMPORTANTE:
     * Usamos EXISTS en vez de JOIN + DISTINCT para evitar:
     * - duplicados por licitación con varios CPVs
     * - error de SQL Server con DISTINCT + ORDER BY
     */
    // ✅ Necesitamos "organismo" en listado para mostrar nombre/NIF
    @EntityGraph(attributePaths = { "cpvs", "organismo" })
    @Query("""
        select l
        from LicitacionEntity l
        where (:estado is null or l.estadoTexto = :estado)
          and (:fromDate is null or l.fechaLimitePresentacion >= :fromDate)
          and (
                :cpvsEmpty = true
                or exists (
                    select 1
                    from l.cpvs c
                    where c.codigo in :cpvs
                )
          )
        order by l.fechaLimitePresentacion asc nulls last,
                 l.fechaPublicacion desc nulls last,
                 l.id desc
    """)
    Page<LicitacionEntity> buscarPublicadas(
            @Param("estado") String estado,
            @Param("fromDate") LocalDate fromDate,
            @Param("cpvs") List<String> cpvs,
            @Param("cpvsEmpty") boolean cpvsEmpty,
            Pageable pageable
    );
}

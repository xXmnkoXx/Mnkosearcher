package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.LicitacionEventoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LicitacionEventoJpaRepository extends JpaRepository<LicitacionEventoEntity, Long> {

    /**
     * Devuelve el ÚLTIMO evento (por licitacion_id) dentro de la ventana y que cumpla el patrón de url_publica.
     * Importante: en tu BD la tabla es "licitacion_eventos" (sin 's').
     *
     * @param fromDt      instante mínimo (últimos N días)
     * @param urlLike     patrón LIKE para url_publica (por ejemplo "%contratacion.euskadi.eus%")
     */
    @Query(value = """
        ;WITH ult AS (
            SELECT
                e.licitacion_id,
                MAX(COALESCE(e.atom_updated_at, e.fecha)) AS last_dt
            FROM licitacion_eventos e
            WHERE COALESCE(e.atom_updated_at, e.fecha) >= :fromDt
              AND e.url_publica LIKE :urlLike
            GROUP BY e.licitacion_id
        )
        SELECT e.*
        FROM licitacion_eventos e
        JOIN ult u
          ON u.licitacion_id = e.licitacion_id
         AND u.last_dt = COALESCE(e.atom_updated_at, e.fecha)
        """, nativeQuery = true)
    List<LicitacionEventoEntity> findEuskadiUltimosEventosPorLicitacion(
            @Param("fromDt") Instant fromDt,
            @Param("urlLike") String urlLike
    );

    Optional<LicitacionEventoEntity> findByAtomEntryId(String atomEntryId);
}

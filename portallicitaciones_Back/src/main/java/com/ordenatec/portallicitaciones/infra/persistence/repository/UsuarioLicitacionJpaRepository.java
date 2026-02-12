package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioLicitacionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UsuarioLicitacionJpaRepository extends JpaRepository<UsuarioLicitacionEntity, Integer> {

    // ------- (lo que ya tenías / útil) -------
    List<UsuarioLicitacionEntity> findByUsuario_IdUsuarioAndFechaEnvioOrderByFechaCreacionDesc(
            Integer idUsuario, LocalDate fechaEnvio
    );

    long countByUsuario_IdUsuarioAndFechaEnvio(Integer idUsuario, LocalDate fechaEnvio);

    void deleteByUsuario_IdUsuarioAndFechaEnvio(Integer idUsuario, LocalDate fechaEnvio);

    // ------- Paginación (histórico completo) -------
    @Query("""
        select ul
        from UsuarioLicitacionEntity ul
        where ul.usuario.idUsuario = :idUsuario
        """)
    Page<UsuarioLicitacionEntity> pageByUsuario(
            @Param("idUsuario") Integer idUsuario,
            Pageable pageable
    );

    @Query("""
        select ul
        from UsuarioLicitacionEntity ul
        where ul.usuario.idUsuario = :idUsuario
          and ul.tipo = :tipo
        """)
    Page<UsuarioLicitacionEntity> pageByUsuarioAndTipo(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") String tipo,
            Pageable pageable
    );

    // q busca en titulo u organismo (case-insensitive)
    @Query("""
        select ul
        from UsuarioLicitacionEntity ul
        where ul.usuario.idUsuario = :idUsuario
          and (
            lower(ul.titulo) like lower(concat('%', :q, '%'))
            or lower(ul.organismo) like lower(concat('%', :q, '%'))
          )
        """)
    Page<UsuarioLicitacionEntity> pageByUsuarioAndQuery(
            @Param("idUsuario") Integer idUsuario,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
        select ul
        from UsuarioLicitacionEntity ul
        where ul.usuario.idUsuario = :idUsuario
          and ul.tipo = :tipo
          and (
            lower(ul.titulo) like lower(concat('%', :q, '%'))
            or lower(ul.organismo) like lower(concat('%', :q, '%'))
          )
        """)
    Page<UsuarioLicitacionEntity> pageByUsuarioTipoAndQuery(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") String tipo,
            @Param("q") String q,
            Pageable pageable
    );
}

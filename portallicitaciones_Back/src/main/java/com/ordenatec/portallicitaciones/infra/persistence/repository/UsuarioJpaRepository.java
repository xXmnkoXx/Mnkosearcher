package com.ordenatec.portallicitaciones.infra.persistence.repository;

import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Integer> {

    Optional<UsuarioEntity> findByUsername(String username);

    // Mantengo el tuyo (por compatibilidad con código existente)
    Optional<UsuarioEntity> findByEmail(String email);

    // ✅ Para ingestión (n8n): comparar ignorando mayúsculas/minúsculas
    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);

    // ✅ Para ingestión (n8n): si el usuario recibe en email_destino
    Optional<UsuarioEntity> findByEmailDestinoIgnoreCase(String emailDestino);

    boolean existsByUsername(String username);

    // Mantengo el tuyo
    boolean existsByEmail(String email);

    // ✅ Variante ignore case (útil si más adelante la usas)
    boolean existsByEmailIgnoreCase(String email);

    // ✅ Para admin: cargar empresa en la misma query (evita LazyInitialization + N+1)
    @Query("select u from UsuarioEntity u left join fetch u.empresa")
    List<UsuarioEntity> findAllWithEmpresa();
}

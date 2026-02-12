package com.ordenatec.portallicitaciones.infra.security;

import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioJpaRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioJpaRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UsuarioEntity u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String rol = (u.getRol() == null || u.getRol().isBlank()) ? "USER" : u.getRol();

        // Spring Security espera ROLE_ADMIN / ROLE_USER cuando usas hasRole("ADMIN")
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + rol)
        );

        return new User(
                u.getUsername(),
                u.getPasswordHash(),
                u.getActivo() != null ? u.getActivo() : true,
                true,
                true,
                true,
                authorities
        );
    }
}

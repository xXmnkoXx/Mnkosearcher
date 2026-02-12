package com.ordenatec.portallicitaciones.config;

import com.ordenatec.portallicitaciones.infra.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ✅ CORS habilitado
            .cors(Customizer.withDefaults())

            // ✅ API stateless
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ✅ Autorización
            .authorizeHttpRequests(auth -> auth

                // ✅ preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ✅ permitir /error para evitar Whitelabel por bloqueos
                .requestMatchers("/error").permitAll()

                // =========================
                // 🔓 IMPORTACIONES SIN TOKEN (TODAS)
                // =========================
                .requestMatchers("/api/importaciones/**").permitAll()

                // =========================
                // 🔓 INGESTIÓN N8N (si lo usas)
                // =========================
                .requestMatchers("/ingestion/**").permitAll()

                // =========================
                // 🔓 ENDPOINT PUBLICO N8N (ya existente)
                // =========================
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/licitaciones/publicadas",
                    "/api/licitaciones/publicadas/**"
                ).permitAll()

                // =========================
                // ✅ (FIX) HACER PUBLICO EL LISTADO GENERAL PARA EL FRONT
                // =========================
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/licitaciones",
                    "/api/licitaciones/**"
                ).permitAll()

                // =========================
                // 🔓 públicos existentes
                // =========================
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health/**").permitAll()

                // =========================
                // 🔐 admin solo ADMIN
                // =========================
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // =========================
                // 🔐 protegidos
                // =========================
                .requestMatchers("/api/alertas/**").authenticated()

                // ✅ el resto de api requiere login
                .requestMatchers("/api/**").authenticated()

                // =========================
                // 🌍 estáticos / front
                // =========================
                .anyRequest().permitAll()
            )

            // ✅ Filtro JWT antes del auth por usuario/contraseña
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}

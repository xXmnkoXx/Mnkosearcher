package com.ordenatec.portallicitaciones.infra.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * Obtiene el username del usuario autenticado a partir del JWT
     */
    public static String getCurrentUsername() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null) {
            return null;
        }

        if (!authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }
}

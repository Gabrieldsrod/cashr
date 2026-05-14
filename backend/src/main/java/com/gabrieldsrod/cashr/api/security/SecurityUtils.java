package com.gabrieldsrod.cashr.api.security;

import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public class SecurityUtils {

    public static UUID getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return UUID.fromString((String) principal);
    }
}

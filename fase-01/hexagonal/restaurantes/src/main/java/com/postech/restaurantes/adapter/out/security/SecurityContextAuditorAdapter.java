package com.postech.restaurantes.adapter.out.security;

import com.postech.restaurantes.application.port.out.AuditorPort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Adapter de saída que realiza o {@link AuditorPort} lendo o usuário autenticado
 * do contexto de segurança do Spring.
 *
 * <p>O fallback para {@code "system"} cobre as gravações sem usuário autenticado:
 * seeds e o autocadastro público, que por definição acontece antes de existir uma
 * sessão.</p>
 */
@Component
public class SecurityContextAuditorAdapter implements AuditorPort {

    private static final String SYSTEM_AUDITOR = "system";

    @Override
    public String currentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_AUDITOR;
        }
        return authentication.getName();
    }
}

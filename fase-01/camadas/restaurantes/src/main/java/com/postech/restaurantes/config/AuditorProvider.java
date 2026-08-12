package com.postech.restaurantes.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolve quem está gravando o registro, para as colunas de auditoria
 * (created_by / last_updated_by): o login do usuário autenticado, com fallback
 * para "system" quando não há usuário autenticado (seeds, auto-cadastro público).
 *
 * Antes esta resolução era um AuditorAware consumido pelo Spring Data JPA. Sem
 * JPA não existe mais esse gancho automático, então os repositórios chamam este
 * componente explicitamente antes de cada INSERT/UPDATE.
 */
@Component
public class AuditorProvider {

    private static final String SYSTEM_AUDITOR = "system";

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

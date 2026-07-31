package com.postech.restaurantes.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpringSecurityAuditorAware — resolução do auditor")
class SpringSecurityAuditorAwareTest {

    private final SpringSecurityAuditorAware auditorAware = new SpringSecurityAuditorAware();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sem Authentication no contexto, audita como \"system\"")
    void semAutenticacao_deveAuditarComoSystem() {
        assertThat(auditorAware.getCurrentAuditor()).contains("system");
    }

    /**
     * O AnonymousAuthenticationFilter popula o contexto num endpoint público
     * (ex.: auto-cadastro) com um token cujo isAuthenticated() é true e cujo
     * nome é "anonymousUser" — que não deve vazar para as colunas de auditoria.
     */
    @Test
    @DisplayName("com Authentication anônima, audita como \"system\"")
    void comAutenticacaoAnonima_deveAuditarComoSystem() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(auditorAware.getCurrentAuditor()).contains("system");
    }

    @Test
    @DisplayName("com usuário autenticado, audita com o login")
    void comUsuarioAutenticado_deveAuditarComOLogin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cliente.demo", null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        assertThat(auditorAware.getCurrentAuditor()).contains("cliente.demo");
    }
}

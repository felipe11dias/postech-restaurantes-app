package com.postech.restaurantes.adapter.in.web.security;

import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Suporte à autorização por posse do recurso, exposto como bean
 * {@code resourceOwner} para uso nas expressões {@code @PreAuthorize}.
 *
 * <p>Regra: um usuário só acessa ou altera o próprio registro; ROLE_ADMIN acessa
 * qualquer um (verificado à parte, por {@code hasRole} na mesma expressão).</p>
 *
 * <p>Autorização é decisão de borda, não de negócio — quem pode chamar o caso de
 * uso é uma questão do adapter que o expõe. Por isso esta classe vive no adapter
 * web e não no núcleo, e por isso ela consome o {@code LoadUserPort} como qualquer
 * outro cliente da aplicação.</p>
 */
@Component("resourceOwner")
public class ResourceOwnerChecker {

    private final LoadUserPort loadUserPort;

    public ResourceOwnerChecker(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    /** True se o usuário autenticado é o dono do recurso identificado por {@code id}. */
    public boolean isSelf(UUID id, Authentication authentication) {
        if (id == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return loadUserPort.findByLogin(authentication.getName())
                .map(User::getId)
                .map(id::equals)
                .orElse(false);
    }
}

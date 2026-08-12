package com.postech.restaurantes.security;

import com.postech.restaurantes.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Suporte à autorização por posse do recurso. Exposto como bean "userSecurity"
 * para uso em expressões @PreAuthorize dos controllers.
 *
 * Regra: um usuário só pode acessar/alterar o próprio registro; ROLE_ADMIN pode
 * acessar qualquer um (verificado à parte, via hasRole na expressão).
 */
@Component("userSecurity")
public class UserSecurity {

    private final UserRepository userRepository;

    public UserSecurity(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** True se o usuário autenticado é o dono do recurso identificado por {@code id}. */
    public boolean isSelf(UUID id, Authentication authentication) {
        if (id == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return userRepository.findByLogin(authentication.getName())
                .map(user -> user.getId().equals(id))
                .orElse(false);
    }
}

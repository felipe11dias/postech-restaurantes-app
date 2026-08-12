package com.postech.restaurantes.repository;

import com.postech.restaurantes.entity.PasswordResetToken;

import java.util.Optional;

/**
 * Contrato de persistência dos tokens de redefinição de senha.
 */
public interface PasswordResetTokenRepository {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Insere quando o id é nulo, atualiza caso contrário. */
    PasswordResetToken save(PasswordResetToken token);
}

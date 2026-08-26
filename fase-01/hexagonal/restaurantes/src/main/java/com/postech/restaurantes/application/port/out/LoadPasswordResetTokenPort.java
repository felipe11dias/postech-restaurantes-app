package com.postech.restaurantes.application.port.out;

import com.postech.restaurantes.domain.model.PasswordResetToken;

import java.util.Optional;

/** Port de saída: leitura de tokens de redefinição de senha pelo hash. */
public interface LoadPasswordResetTokenPort {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}

package com.postech.restaurantes.application.port.out;

import com.postech.restaurantes.domain.model.PasswordResetToken;

/** Port de saída: gravação de tokens de redefinição de senha. */
public interface SavePasswordResetTokenPort {

    PasswordResetToken save(PasswordResetToken token);
}

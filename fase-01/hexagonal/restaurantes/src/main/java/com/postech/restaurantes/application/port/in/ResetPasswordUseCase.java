package com.postech.restaurantes.application.port.in;

import com.postech.restaurantes.application.port.in.command.ResetPasswordCommand;

/** Caso de uso: redefinir a senha a partir de um token de recuperação. */
public interface ResetPasswordUseCase {

    void resetPassword(ResetPasswordCommand command);
}

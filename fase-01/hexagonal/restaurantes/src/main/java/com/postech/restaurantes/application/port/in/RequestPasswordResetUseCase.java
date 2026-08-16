package com.postech.restaurantes.application.port.in;

import com.postech.restaurantes.application.port.in.command.RequestPasswordResetCommand;

/** Caso de uso: solicitar a recuperação de senha, gerando e enviando um token. */
public interface RequestPasswordResetUseCase {

    void requestReset(RequestPasswordResetCommand command);
}

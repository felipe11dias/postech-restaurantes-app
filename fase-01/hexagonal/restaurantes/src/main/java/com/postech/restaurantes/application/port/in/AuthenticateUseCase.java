package com.postech.restaurantes.application.port.in;

import com.postech.restaurantes.application.port.in.command.AuthenticateCommand;
import com.postech.restaurantes.application.port.in.view.AuthView;

/** Caso de uso: validar credenciais e emitir um token de acesso. */
public interface AuthenticateUseCase {

    AuthView authenticate(AuthenticateCommand command);
}

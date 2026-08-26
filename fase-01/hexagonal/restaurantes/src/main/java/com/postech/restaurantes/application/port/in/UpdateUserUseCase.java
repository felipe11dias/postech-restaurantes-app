package com.postech.restaurantes.application.port.in;

import com.postech.restaurantes.application.port.in.command.UpdateUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;

/** Caso de uso: atualizar os dados cadastrais do usuário (nunca a senha). */
public interface UpdateUserUseCase {

    UserView update(UpdateUserCommand command);
}

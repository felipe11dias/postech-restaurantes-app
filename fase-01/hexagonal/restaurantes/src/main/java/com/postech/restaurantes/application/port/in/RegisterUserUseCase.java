package com.postech.restaurantes.application.port.in;

import com.postech.restaurantes.application.port.in.command.RegisterUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;

/**
 * Caso de uso: cadastrar um novo usuário (dono de restaurante ou cliente).
 *
 * <p>Um <em>input port</em> por caso de uso, e não um "UserUseCase" com sete
 * métodos: é a Segregação de Interfaces aplicada à borda de entrada. O controller
 * que só cadastra depende só disto, e um teste que exercita o cadastro não precisa
 * fabricar dublês para exclusão, busca e login.</p>
 */
public interface RegisterUserUseCase {

    UserView register(RegisterUserCommand command);
}

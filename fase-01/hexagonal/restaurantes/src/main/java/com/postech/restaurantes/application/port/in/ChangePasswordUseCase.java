package com.postech.restaurantes.application.port.in;

import com.postech.restaurantes.application.port.in.command.ChangePasswordCommand;

/**
 * Caso de uso: trocar a senha informando a senha atual.
 *
 * <p>Ser um port próprio — e não um método a mais na atualização de cadastro — é
 * o que atende, já no desenho, ao requisito de endpoint exclusivo de senha. A
 * rota {@code PATCH /users/{id}/password} é consequência desta separação, não a
 * causa dela.</p>
 */
public interface ChangePasswordUseCase {

    void changePassword(ChangePasswordCommand command);
}

package com.postech.restaurantes.application.port.in;

import java.util.UUID;

/** Caso de uso: excluir um usuário. */
public interface DeleteUserUseCase {

    void delete(UUID userId);
}

package com.postech.restaurantes.application.port.out;

import java.util.UUID;

/** Port de saída: exclusão de usuários (papéis e endereços saem junto). */
public interface DeleteUserPort {

    void deleteById(UUID id);
}

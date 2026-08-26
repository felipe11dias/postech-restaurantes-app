package com.postech.restaurantes.application.port.out;

import com.postech.restaurantes.domain.model.User;

/**
 * Port de saída: gravação de usuários.
 *
 * <p>O usuário é a raiz do agregado — salvar um usuário também sincroniza seus
 * papéis e endereços. Como o núcleo trata isso como uma operação única, é
 * responsabilidade do adapter garantir que ela seja atômica.</p>
 */
public interface SaveUserPort {

    /** Insere quando o usuário ainda não tem id, atualiza caso contrário. */
    User save(User user);
}

package com.postech.restaurantes.domain.exception;

/**
 * Lançada quando se tenta criar/atualizar um recurso com um valor único já
 * existente (e-mail ou login). O domínio não sabe que isso vira um HTTP 409 —
 * essa tradução é do adapter de entrada web.
 */
public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException email(String email) {
        return new DuplicateResourceException("Já existe um usuário com o e-mail " + email);
    }

    public static DuplicateResourceException login(String login) {
        return new DuplicateResourceException("Já existe um usuário com o login " + login);
    }
}

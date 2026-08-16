package com.postech.restaurantes.domain.exception;

/**
 * Lançada pelo Value Object {@code Email} quando o valor informado não é um
 * e-mail válido — inclusive quando está ausente.
 */
public class InvalidEmailException extends DomainException {

    private InvalidEmailException(String message) {
        super(message);
    }

    public static InvalidEmailException required() {
        return new InvalidEmailException("O e-mail é obrigatório");
    }

    public static InvalidEmailException malformed(String email) {
        return new InvalidEmailException("E-mail inválido: " + email);
    }
}

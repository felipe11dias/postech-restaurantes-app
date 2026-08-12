package com.postech.restaurantes.domain.exception;

public class InvalidEmailException extends DomainException {
    public InvalidEmailException(String email) {
        super(String.format("Email inválido: %s", email));
    }
}

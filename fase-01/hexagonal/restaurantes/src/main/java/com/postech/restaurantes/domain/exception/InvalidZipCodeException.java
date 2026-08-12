package com.postech.restaurantes.domain.exception;

public class InvalidZipCodeException extends DomainException {
    public InvalidZipCodeException(String zipCode) {
        super(String.format("CEP inválido: %s", zipCode));
    }
}

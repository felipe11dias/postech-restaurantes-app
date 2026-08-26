package com.postech.restaurantes.domain.exception;

/**
 * Lançada pelo Value Object {@code ZipCode} quando o valor informado não tem os
 * 8 dígitos de um CEP.
 */
public class InvalidZipCodeException extends DomainException {

    public InvalidZipCodeException(String zipCode) {
        super("CEP inválido: " + zipCode + " (esperado 8 dígitos)");
    }
}

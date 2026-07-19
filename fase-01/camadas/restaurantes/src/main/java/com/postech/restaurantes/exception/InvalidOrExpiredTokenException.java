package com.postech.restaurantes.exception;

/**
 * Lançada na redefinição de senha quando o token informado não existe, já
 * expirou ou já foi utilizado. Mapeada para HTTP 400.
 */
public class InvalidOrExpiredTokenException extends RuntimeException {
    public InvalidOrExpiredTokenException(String message) {
        super(message);
    }
}

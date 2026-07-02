package com.postech.restaurantes.exception;

/**
 * Lançada na troca de senha quando a senha atual está incorreta ou a
 * confirmação não coincide. Mapeada para HTTP 400 na Etapa 9.
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}

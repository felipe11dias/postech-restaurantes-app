package com.postech.restaurantes.domain.exception;

/**
 * Lançada na troca de senha quando a senha atual está incorreta ou a
 * confirmação não coincide com a nova senha.
 */
public class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(String message) {
        super(message);
    }

    public static InvalidPasswordException currentPasswordMismatch() {
        return new InvalidPasswordException("A senha atual está incorreta");
    }

    public static InvalidPasswordException confirmationMismatch() {
        return new InvalidPasswordException("A confirmação não coincide com a nova senha");
    }
}

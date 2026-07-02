package com.postech.restaurantes.exception;

/**
 * Lançada quando um recurso solicitado não existe (usuário, papel).
 * Mapeada para HTTP 404 na Etapa 9.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

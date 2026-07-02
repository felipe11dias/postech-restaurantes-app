package com.postech.restaurantes.exception;

/**
 * Lançada quando se tenta criar/atualizar um recurso com um valor único já
 * existente (e-mail ou login). Mapeada para HTTP 409 na Etapa 9.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

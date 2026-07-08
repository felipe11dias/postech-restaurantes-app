package com.postech.restaurantes.exception;

/**
 * Lançada quando uma operação é solicitada sem a autorização necessária no nível
 * de regra de negócio (ex.: tentar se autocadastrar com um papel privilegiado).
 * Mapeada para HTTP 403 na Etapa 9.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}

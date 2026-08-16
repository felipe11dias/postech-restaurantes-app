package com.postech.restaurantes.domain.exception;

import com.postech.restaurantes.domain.model.RoleName;

import java.util.UUID;

/**
 * Lançada quando um recurso solicitado não existe (usuário, papel).
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException user(UUID id) {
        return new ResourceNotFoundException("Usuário não encontrado: " + id);
    }

    public static ResourceNotFoundException role(RoleName name) {
        return new ResourceNotFoundException("Papel não encontrado: " + name);
    }
}

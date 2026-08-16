package com.postech.restaurantes.domain.exception;

import com.postech.restaurantes.domain.model.RoleName;

/**
 * Lançada quando uma operação é vetada por regra de negócio — não por falta de
 * autenticação. O caso concreto desta fase é o autocadastro tentando reivindicar
 * um papel privilegiado.
 */
public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    public static ForbiddenOperationException selfRegistrationWithRole(RoleName roleName) {
        return new ForbiddenOperationException(
                "Não é permitido se autocadastrar com o papel " + roleName);
    }
}

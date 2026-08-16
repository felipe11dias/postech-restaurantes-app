package com.postech.restaurantes.domain.exception;

/**
 * Lançada na redefinição de senha quando o token informado não existe, já
 * expirou ou já foi utilizado.
 *
 * <p>A mensagem é deliberadamente a mesma nos três casos: distinguir "não existe"
 * de "expirado" entregaria a quem tenta adivinhar um token a informação de que
 * acertou o valor.</p>
 */
public class InvalidOrExpiredTokenException extends DomainException {

    private static final String MESSAGE = "Token inválido ou expirado";

    public InvalidOrExpiredTokenException() {
        super(MESSAGE);
    }
}

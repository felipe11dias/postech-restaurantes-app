package com.postech.restaurantes.domain.exception;

/**
 * Raiz das exceções de negócio do sistema.
 *
 * <p>Toda falha que o domínio sabe nomear ("e-mail duplicado", "senha atual
 * incorreta") desce daqui. O núcleo lança essas exceções sem saber que existe
 * HTTP; quem as traduz em status e ProblemDetail é o adapter de entrada web.
 * Ter uma raiz comum é o que permite ao adapter distinguir, em um único ponto,
 * um erro de negócio previsto de um defeito inesperado — que vira 500 e vai
 * para o log.</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

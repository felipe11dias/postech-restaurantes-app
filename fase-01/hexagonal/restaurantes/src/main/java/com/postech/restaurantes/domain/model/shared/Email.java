package com.postech.restaurantes.domain.model.shared;

import com.postech.restaurantes.domain.exception.InvalidEmailException;
import com.postech.restaurantes.domain.util.TextUtils;

import java.util.regex.Pattern;

/**
 * Value Object de e-mail do domínio.
 *
 * <p>Garante a invariante de formato e normaliza para minúsculas na construção.
 * A normalização não é cosmética: é ela que sustenta a regra de negócio de e-mail
 * único, porque faz {@code Joao@Email.com} e {@code joao@email.com} compararem
 * iguais antes de a checagem de duplicidade acontecer. Sem isso, a mesma pessoa
 * conseguiria se cadastrar duas vezes só variando a caixa das letras.</p>
 *
 * <p>Sendo do domínio, não conhece Jackson nem JPA — quem converte JSON em
 * {@code Email} é o adapter de entrada.</p>
 */
public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");

    public Email {
        value = TextUtils.toLowerNormalized(value);
        if (value == null || value.isBlank()) {
            throw InvalidEmailException.required();
        }
        if (!PATTERN.matcher(value).matches()) {
            throw InvalidEmailException.malformed(value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

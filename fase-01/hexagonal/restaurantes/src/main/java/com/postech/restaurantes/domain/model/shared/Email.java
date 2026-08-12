package com.postech.restaurantes.domain.model.shared;

import com.postech.restaurantes.domain.exception.InvalidEmailException;
import com.postech.restaurantes.domain.util.TextUtils;

import java.util.regex.Pattern;

/**
 * Value Object de e-mail do domínio. Garante a invariante de formato e normaliza
 * para minúsculas na construção — normalização que sustenta a regra de negócio
 * de e-mail único (duas grafias do mesmo endereço comparam iguais).
 */
public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");

    public Email {
        value = TextUtils.toLowerNormalized(value);
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("E-mail é obrigatório");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException(String.format("E-mail: %s é inválido", value));
        }
    }
}

package com.postech.restaurantes.vo.shared;

import com.postech.restaurantes.util.TextUtils;

import java.util.regex.Pattern;

/**
 * Value Object de e-mail. Compartilhado entre versões da API (não versionado),
 * por ser um conceito estável de domínio. Valida e normaliza na construção.
 */
public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");

    public Email {
        value = TextUtils.toLowerNormalized(value);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("E-mail inválido");
        }
    }
}

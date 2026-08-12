package com.postech.restaurantes.domain.model.shared;

import com.postech.restaurantes.domain.exception.InvalidZipCodeException;
import com.postech.restaurantes.domain.util.TextUtils;

/**
 * Value Object de CEP do domínio. Garante a invariante de exatamente 8 dígitos,
 * normalizando na construção (aceita entrada com ou sem máscara) e expondo a
 * forma mascarada sob demanda via {@link #formatted()}.
 */
public record ZipCode(String value) {

    public ZipCode {
        String digits = TextUtils.onlyDigits(value);
        if (digits == null || digits.length() != 8) {
            throw new InvalidZipCodeException(String.format("CEP: %s é inválido (esperado 8 dígitos)", value));
        }
        value = digits;
    }

    /** Retorna o CEP no formato 00000-000. */
    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }
}

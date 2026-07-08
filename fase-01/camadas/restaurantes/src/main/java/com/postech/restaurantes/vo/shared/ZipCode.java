package com.postech.restaurantes.vo.shared;

import com.postech.restaurantes.util.TextUtils;

/**
 * Value Object de CEP. Armazena apenas os 8 dígitos e formata sob demanda.
 * Compartilhado entre versões da API (não versionado).
 */
public record ZipCode(String value) {

    public ZipCode {
        String digits = TextUtils.onlyDigits(value);
        if (digits == null || digits.length() != 8) {
            throw new IllegalArgumentException("CEP inválido (esperado 8 dígitos)");
        }
        value = digits;
    }

    /** Retorna o CEP no formato 00000-000. */
    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }
}

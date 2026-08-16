package com.postech.restaurantes.domain.model.shared;

import com.postech.restaurantes.domain.exception.InvalidZipCodeException;
import com.postech.restaurantes.domain.util.TextUtils;

/**
 * Value Object de CEP do domínio.
 *
 * <p>Garante a invariante de exatamente 8 dígitos, normalizando na construção
 * (aceita entrada com ou sem máscara) e expondo a forma mascarada sob demanda via
 * {@link #formatted()}. Guardar sempre os dígitos crus e formatar só na saída evita
 * ter dois CEPs iguais gravados de formas diferentes.</p>
 */
public record ZipCode(String value) {

    private static final int LENGTH = 8;

    public ZipCode {
        String digits = TextUtils.onlyDigits(value);
        if (digits == null || digits.length() != LENGTH) {
            throw new InvalidZipCodeException(value);
        }
        value = digits;
    }

    /** Retorna o CEP no formato 00000-000. */
    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }

    @Override
    public String toString() {
        return formatted();
    }
}

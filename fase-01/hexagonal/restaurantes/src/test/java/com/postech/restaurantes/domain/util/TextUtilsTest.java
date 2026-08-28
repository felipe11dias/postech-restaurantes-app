package com.postech.restaurantes.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("TextUtils — normalização de texto do domínio")
class TextUtilsTest {

    @Test
    @DisplayName("normalize apara as pontas e colapsa espaços internos")
    void normalize() {
        assertEquals("Maria da Silva", TextUtils.normalize("  Maria   da   Silva  "));
        assertNull(TextUtils.normalize(null));
    }

    /** É esta normalização que sustenta a regra de e-mail único. */
    @Test
    @DisplayName("toLowerNormalized normaliza e converte para minúsculas")
    void toLowerNormalized() {
        assertEquals("maria@email.com", TextUtils.toLowerNormalized("  MARIA@Email.COM "));
        assertNull(TextUtils.toLowerNormalized(null));
    }

    @Test
    @DisplayName("removeAccents remove acentos e diacríticos")
    void removeAccents() {
        assertEquals("Sao Paulo", TextUtils.removeAccents("São Paulo"));
        assertEquals("Ceara", TextUtils.removeAccents("Ceará"));
        assertNull(TextUtils.removeAccents(null));
    }

    @Test
    @DisplayName("onlyDigits mantém apenas dígitos")
    void onlyDigits() {
        assertEquals("01310200", TextUtils.onlyDigits("01310-200"));
        assertEquals("11999990000", TextUtils.onlyDigits("(11) 99999-0000"));
        assertNull(TextUtils.onlyDigits(null));
    }
}

package com.postech.restaurantes.domain.model.shared;

import com.postech.restaurantes.domain.exception.InvalidZipCodeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipCodeTest {

    @Test
    @DisplayName("aceita CEP com máscara e guarda apenas os dígitos")
    void aceitaComMascara() {
        assertEquals("01001000", new ZipCode("01001-000").value());
    }

    @Test
    @DisplayName("aceita CEP sem máscara")
    void aceitaSemMascara() {
        assertEquals("01001000", new ZipCode("01001000").value());
    }

    @Test
    @DisplayName("com e sem máscara produzem o mesmo valor — não há CEP duplicado por formatação")
    void mesmaEntradaFormatadaDiferente() {
        assertEquals(new ZipCode("01001-000"), new ZipCode("01001000"));
    }

    @Test
    @DisplayName("formata sob demanda no padrão 00000-000")
    void formata() {
        assertEquals("01001-000", new ZipCode("01001000").formatted());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789", "abcdefgh"})
    @DisplayName("rejeita CEP que não tenha exatamente 8 dígitos")
    void rejeitaTamanhoInvalido(String valor) {
        assertThrows(InvalidZipCodeException.class, () -> new ZipCode(valor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("rejeita CEP ausente")
    void rejeitaAusente(String valor) {
        assertThrows(InvalidZipCodeException.class, () -> new ZipCode(valor));
    }

    @Test
    @DisplayName("toString devolve o CEP mascarado")
    void toStringDevolveMascarado() {
        assertEquals("01001-000", new ZipCode("01001000").toString());
    }
}

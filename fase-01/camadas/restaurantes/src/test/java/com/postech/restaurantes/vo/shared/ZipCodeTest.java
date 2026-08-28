package com.postech.restaurantes.vo.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ZipCode — VO de CEP")
class ZipCodeTest {

    @Test
    @DisplayName("guarda apenas os 8 dígitos, com ou sem máscara na entrada")
    void deveGuardarApenasDigitos() {
        assertThat(new ZipCode("60175-047").value()).isEqualTo("60175047");
        assertThat(new ZipCode("60175047").value()).isEqualTo("60175047");
    }

    @Test
    @DisplayName("formatted devolve o CEP mascarado")
    void formatted_deveMascarar() {
        assertThat(new ZipCode("60175047").formatted()).isEqualTo("60175-047");
    }

    @ParameterizedTest(name = "\"{0}\" é rejeitado")
    @ValueSource(strings = {"", "1234567", "123456789", "abcdefgh"})
    @DisplayName("rejeita CEP que não tenha exatamente 8 dígitos")
    void quantidadeInvalidaDeDigitos_deveLancar(String valor) {
        assertThatThrownBy(() -> new ZipCode(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CEP inválido (esperado 8 dígitos)");
    }

    @Test
    @DisplayName("rejeita CEP nulo")
    void nulo_deveLancar() {
        assertThatThrownBy(() -> new ZipCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CEP inválido (esperado 8 dígitos)");
    }
}

package com.postech.restaurantes.vo.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email — VO de e-mail")
class EmailTest {

    @Test
    @DisplayName("normaliza para minúsculas e apara espaços na construção")
    void deveNormalizarNaConstrucao() {
        assertThat(new Email("  JOAO.Silva+tag@Email.com.BR ").value())
                .isEqualTo("joao.silva+tag@email.com.br");
    }

    @Test
    @DisplayName("aceita e-mail válido")
    void deveAceitarEmailValido() {
        assertThat(new Email("cliente-demo@email.com").value()).isEqualTo("cliente-demo@email.com");
    }

    @ParameterizedTest(name = "\"{0}\" é rejeitado como obrigatório")
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejeita e-mail em branco")
    void emBranco_deveLancar(String valor) {
        assertThatThrownBy(() -> new Email(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("E-mail é obrigatório");
    }

    @Test
    @DisplayName("rejeita e-mail nulo")
    void nulo_deveLancar() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("E-mail é obrigatório");
    }

    @ParameterizedTest(name = "\"{0}\" é rejeitado por formato")
    @ValueSource(strings = {"sem-arroba", "sem@dominio", "@email.com", "joao@@email.com"})
    @DisplayName("rejeita e-mail com formato inválido")
    void formatoInvalido_deveLancar(String valor) {
        assertThatThrownBy(() -> new Email(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("E-mail inválido");
    }
}

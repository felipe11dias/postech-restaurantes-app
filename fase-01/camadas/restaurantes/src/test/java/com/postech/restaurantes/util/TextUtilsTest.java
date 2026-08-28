package com.postech.restaurantes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextUtils — normalização de texto")
class TextUtilsTest {

    @Test
    @DisplayName("normalize apara as pontas e colapsa espaços internos")
    void normalize_deveColapsarEspacos() {
        assertThat(TextUtils.normalize("  João   da   Silva  ")).isEqualTo("João da Silva");
        assertThat(TextUtils.normalize(null)).isNull();
    }

    @Test
    @DisplayName("toLowerNormalized normaliza e converte para minúsculas")
    void toLowerNormalized_deveConverterParaMinusculas() {
        assertThat(TextUtils.toLowerNormalized("  JOAO@Email.COM ")).isEqualTo("joao@email.com");
        assertThat(TextUtils.toLowerNormalized(null)).isNull();
    }

    @Test
    @DisplayName("removeAccents remove acentos e diacríticos")
    void removeAccents_deveRemoverDiacriticos() {
        assertThat(TextUtils.removeAccents("São Paulo")).isEqualTo("Sao Paulo");
        assertThat(TextUtils.removeAccents("Ceará")).isEqualTo("Ceara");
        assertThat(TextUtils.removeAccents(null)).isNull();
    }

    @Test
    @DisplayName("onlyDigits mantém apenas dígitos")
    void onlyDigits_deveManterApenasDigitos() {
        assertThat(TextUtils.onlyDigits("60175-047")).isEqualTo("60175047");
        assertThat(TextUtils.onlyDigits("(85) 99999-0000")).isEqualTo("85999990000");
        assertThat(TextUtils.onlyDigits(null)).isNull();
    }

    @Test
    @DisplayName("não é instanciável fora da própria classe")
    void naoDeveSerInstanciavel() throws Exception {
        Constructor<TextUtils> constructor = TextUtils.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();

        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}

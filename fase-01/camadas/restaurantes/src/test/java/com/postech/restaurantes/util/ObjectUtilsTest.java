package com.postech.restaurantes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ObjectUtils — guardas de presença de valores")
class ObjectUtilsTest {

    @Nested
    @DisplayName("isBlank / isNotBlank")
    class Texto {

        @Test
        @DisplayName("nulo, vazio e só espaços são considerados em branco")
        void deveDetectarTextoEmBranco() {
            assertThat(ObjectUtils.isBlank(null)).isTrue();
            assertThat(ObjectUtils.isBlank("")).isTrue();
            assertThat(ObjectUtils.isBlank("   ")).isTrue();
        }

        @Test
        @DisplayName("texto com conteúdo não está em branco")
        void deveDetectarTextoPreenchido() {
            assertThat(ObjectUtils.isBlank("João")).isFalse();
            assertThat(ObjectUtils.isNotBlank("João")).isTrue();
            assertThat(ObjectUtils.isNotBlank("  ")).isFalse();
        }
    }

    @Nested
    @DisplayName("isEmpty / isNotEmpty")
    class Colecoes {

        @Test
        @DisplayName("nula e vazia são consideradas vazias")
        void deveDetectarColecaoVazia() {
            assertThat(ObjectUtils.isEmpty(null)).isTrue();
            assertThat(ObjectUtils.isEmpty(List.of())).isTrue();
        }

        @Test
        @DisplayName("coleção com elementos não está vazia")
        void deveDetectarColecaoPreenchida() {
            assertThat(ObjectUtils.isEmpty(Set.of("a"))).isFalse();
            assertThat(ObjectUtils.isNotEmpty(Set.of("a"))).isTrue();
            assertThat(ObjectUtils.isNotEmpty(List.of())).isFalse();
        }
    }

    @Nested
    @DisplayName("requireNonNull / requireNonBlank")
    class Exigencias {

        @Test
        @DisplayName("devolve o próprio valor quando ele está presente")
        void deveDevolverValorPresente() {
            assertThat(ObjectUtils.requireNonNull("valor", "erro")).isEqualTo("valor");
            assertThat(ObjectUtils.requireNonBlank("valor", "erro")).isEqualTo("valor");
        }

        @Test
        @DisplayName("lança IllegalArgumentException com a mensagem dada quando o valor é nulo")
        void nulo_deveLancar() {
            assertThatThrownBy(() -> ObjectUtils.requireNonNull(null, "o valor é obrigatório"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("o valor é obrigatório");
        }

        @Test
        @DisplayName("lança IllegalArgumentException quando o texto está em branco")
        void emBranco_deveLancar() {
            assertThatThrownBy(() -> ObjectUtils.requireNonBlank("  ", "o nome é obrigatório"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("o nome é obrigatório");
        }
    }

    /**
     * A classe é um contêiner de funções estáticas; o construtor privado existe
     * justamente para impedir instanciação. Invocá-lo por reflexão comprova que
     * ele está lá e mantém a classe integralmente exercitada pelos testes.
     */
    @Test
    @DisplayName("não é instanciável fora da própria classe")
    void naoDeveSerInstanciavel() throws Exception {
        Constructor<ObjectUtils> constructor = ObjectUtils.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();

        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}

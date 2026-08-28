package com.postech.restaurantes.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As guardas que o domínio usa para cobrar as próprias invariantes. Como estão em
 * {@code domain}, não podem depender de nada externo — e é por isso que existem,
 * em vez de um {@code Objects.requireNonNull} ou de uma anotação de validação.
 */
@DisplayName("ObjectUtils — guardas de presença de valores do domínio")
class ObjectUtilsTest {

    @Nested
    @DisplayName("isBlank / isNotBlank")
    class Texto {

        @Test
        @DisplayName("nulo, vazio e só espaços são considerados em branco")
        void emBranco() {
            assertTrue(ObjectUtils.isBlank(null));
            assertTrue(ObjectUtils.isBlank(""));
            assertTrue(ObjectUtils.isBlank("   "));
        }

        @Test
        @DisplayName("texto com conteúdo não está em branco")
        void preenchido() {
            assertFalse(ObjectUtils.isBlank("Maria"));
            assertTrue(ObjectUtils.isNotBlank("Maria"));
            assertFalse(ObjectUtils.isNotBlank("  "));
        }
    }

    @Nested
    @DisplayName("isEmpty / isNotEmpty")
    class Colecoes {

        @Test
        @DisplayName("nula e vazia são consideradas vazias")
        void vazia() {
            assertTrue(ObjectUtils.isEmpty(null));
            assertTrue(ObjectUtils.isEmpty(List.of()));
        }

        @Test
        @DisplayName("coleção com elementos não está vazia")
        void preenchida() {
            assertFalse(ObjectUtils.isEmpty(Set.of("a")));
            assertTrue(ObjectUtils.isNotEmpty(Set.of("a")));
            assertFalse(ObjectUtils.isNotEmpty(List.of()));
        }
    }

    @Nested
    @DisplayName("requireNonNull / requireNonBlank")
    class Exigencias {

        @Test
        @DisplayName("devolvem o próprio valor quando ele está presente")
        void valorPresente() {
            assertEquals("valor", ObjectUtils.requireNonNull("valor", "erro"));
            assertEquals("valor", ObjectUtils.requireNonBlank("valor", "erro"));
        }

        @Test
        @DisplayName("valor nulo é recusado com a mensagem dada")
        void nulo() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> ObjectUtils.requireNonNull(null, "o e-mail é obrigatório"));

            assertEquals("o e-mail é obrigatório", erro.getMessage());
        }

        @Test
        @DisplayName("texto em branco é recusado com a mensagem dada")
        void emBranco() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> ObjectUtils.requireNonBlank("  ", "o nome é obrigatório"));

            assertEquals("o nome é obrigatório", erro.getMessage());
        }
    }
}

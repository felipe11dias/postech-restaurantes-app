package com.postech.restaurantes.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

@DisplayName("PasswordResetTokenGenerator — token opaco e seu hash")
class PasswordResetTokenGeneratorTest {

    private final PasswordResetTokenGenerator generator = new PasswordResetTokenGenerator();

    @Test
    @DisplayName("gera token em Base64 URL-safe de 32 bytes")
    void generateToken_deveGerarTokenUrlSafe() {
        String token = generator.generateToken();

        assertThat(token).isNotBlank().doesNotContain("=", "+", "/");
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
    }

    /** Token previsível permitiria adivinhar o link de redefinição de outra conta. */
    @Test
    @DisplayName("dois tokens seguidos são diferentes")
    void generateToken_deveGerarValoresDistintos() {
        assertThat(generator.generateToken()).isNotEqualTo(generator.generateToken());
    }

    @Test
    @DisplayName("hash é estável para a mesma entrada")
    void hash_deveSerEstavel() {
        assertThat(generator.hash("token-bruto")).isEqualTo(generator.hash("token-bruto"));
    }

    @Test
    @DisplayName("hash difere entre entradas diferentes e não devolve o token em claro")
    void hash_deveDiferirEntreEntradas() {
        String hash = generator.hash("token-bruto");

        assertThat(hash).isNotEqualTo(generator.hash("outro-token")).isNotEqualTo("token-bruto");
    }

    @Test
    @DisplayName("hash é SHA-256 em Base64 URL-safe sem preenchimento")
    void hash_deveSerSha256UrlSafe() {
        String hash = generator.hash("token-bruto");

        assertThat(hash).doesNotContain("=", "+", "/");
        assertThat(Base64.getUrlDecoder().decode(hash)).hasSize(32);
    }

    /**
     * SHA-256 é obrigatório em qualquer JVM, então o catch do NoSuchAlgorithmException
     * é uma guarda que nunca dispara em produção. Substituir a fábrica do
     * MessageDigest é a única forma de comprovar que a falha vira um erro de
     * estado explícito — e não um token silenciosamente sem hash.
     */
    @Test
    @DisplayName("ausência do algoritmo SHA-256 vira falha explícita de estado")
    void hash_semAlgoritmoDisponivel_deveLancarIllegalState() {
        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("SHA-256 indisponível"));

            assertThatThrownBy(() -> generator.hash("token-bruto"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Algoritmo de hash SHA-256 indisponível")
                    .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }
}

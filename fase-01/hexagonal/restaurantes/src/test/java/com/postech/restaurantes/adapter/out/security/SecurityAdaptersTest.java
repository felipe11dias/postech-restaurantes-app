package com.postech.restaurantes.adapter.out.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

/**
 * Os três adapters de saída menores: hash de senha, geração do token opaco de
 * redefinição e resolução do auditor. Cada um confina uma decisão de tecnologia
 * (BCrypt, SHA-256, SecurityContextHolder) que o núcleo não conhece.
 */
class SecurityAdaptersTest {

    @Nested
    @DisplayName("BCryptPasswordAdapter")
    class Senha {

        private final BCryptPasswordAdapter adapter =
                new BCryptPasswordAdapter(new BCryptPasswordEncoder());

        @Test
        @DisplayName("o hash não devolve a senha em claro e confere com ela")
        void hashConfere() {
            String hash = adapter.encode("senha12345");

            assertNotEquals("senha12345", hash);
            assertTrue(adapter.matches("senha12345", hash));
        }

        @Test
        @DisplayName("senha errada não confere com o hash")
        void senhaErrada() {
            assertFalse(adapter.matches("senha-errada", adapter.encode("senha12345")));
        }
    }

    @Nested
    @DisplayName("SecureRandomResetTokenAdapter")
    class TokenDeRedefinicao {

        private final SecureRandomResetTokenAdapter adapter = new SecureRandomResetTokenAdapter();

        @Test
        @DisplayName("gera token URL-safe com 256 bits de entropia")
        void tokenUrlSafe() {
            String token = adapter.generateToken();

            assertFalse(token.contains("="));
            assertFalse(token.contains("+"));
            assertFalse(token.contains("/"));
            assertEquals(32, Base64.getUrlDecoder().decode(token).length);
        }

        /** Token previsível permitiria adivinhar o link de redefinição de outra conta. */
        @Test
        @DisplayName("dois tokens seguidos são diferentes")
        void tokensDistintos() {
            assertNotEquals(adapter.generateToken(), adapter.generateToken());
        }

        @Test
        @DisplayName("o hash é estável e nunca devolve o token em claro")
        void hashEstavel() {
            String hash = adapter.hash("token-bruto");

            assertEquals(hash, adapter.hash("token-bruto"));
            assertNotEquals("token-bruto", hash);
            assertNotEquals(hash, adapter.hash("outro-token"));
            assertEquals(32, Base64.getUrlDecoder().decode(hash).length);
        }

        /**
         * SHA-256 é obrigatório em qualquer JVM, então este catch nunca dispara em
         * produção. Substituir a fábrica do MessageDigest é a única forma de
         * comprovar que a falha vira erro de estado explícito — e não um token
         * silenciosamente sem hash.
         */
        @Test
        @DisplayName("ausência do SHA-256 vira falha explícita de estado")
        void semAlgoritmoDisponivel() {
            try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
                digest.when(() -> MessageDigest.getInstance("SHA-256"))
                        .thenThrow(new NoSuchAlgorithmException("indisponível"));

                IllegalStateException erro = assertThrows(IllegalStateException.class,
                        () -> adapter.hash("token-bruto"));
                assertEquals("Algoritmo de hash SHA-256 indisponível", erro.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("SecurityContextAuditorAdapter")
    class Auditor {

        private final SecurityContextAuditorAdapter adapter = new SecurityContextAuditorAdapter();

        @AfterEach
        void limparContexto() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("sem autenticação, audita como \"system\"")
        void semAutenticacao() {
            assertEquals("system", adapter.currentAuditor());
        }

        /**
         * O autocadastro é público: o filtro anônimo popula o contexto com um token
         * cujo nome é "anonymousUser", que não pode vazar para a coluna de auditoria.
         */
        @Test
        @DisplayName("com autenticação anônima, audita como \"system\"")
        void autenticacaoAnonima() {
            SecurityContextHolder.getContext().setAuthentication(
                    new AnonymousAuthenticationToken("key", "anonymousUser",
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            assertEquals("system", adapter.currentAuditor());
        }

        @Test
        @DisplayName("com autenticação ainda não confirmada, audita como \"system\"")
        void autenticacaoNaoConfirmada() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("maria.silva", "senha"));

            assertEquals("system", adapter.currentAuditor());
        }

        @Test
        @DisplayName("com usuário autenticado, audita com o login")
        void usuarioAutenticado() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("maria.silva", null,
                            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

            assertEquals("maria.silva", adapter.currentAuditor());
        }
    }
}

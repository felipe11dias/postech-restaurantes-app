package com.postech.restaurantes.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das duas regras que decidem se um token de redefinição ainda vale.
 * Elas moram na entidade, não no serviço — por isso são testáveis assim.
 */
class PasswordResetTokenTest {

    @Test
    @DisplayName("token recém-emitido é utilizável")
    void recemEmitidoEhUtilizavel() {
        PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), "hash", LocalDateTime.now().plusMinutes(30));

        assertTrue(token.isUsable());
        assertFalse(token.isExpired());
        assertFalse(token.isUsed());
    }

    @Test
    @DisplayName("token expirado não é utilizável")
    void expiradoNaoEhUtilizavel() {
        PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), "hash", LocalDateTime.now().minusMinutes(1));

        assertTrue(token.isExpired());
        assertFalse(token.isUsable());
    }

    @Test
    @DisplayName("token já usado não é utilizável, mesmo dentro da validade")
    void usadoNaoEhUtilizavel() {
        PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), "hash", LocalDateTime.now().plusMinutes(30));

        token.markUsed();

        assertTrue(token.isUsed());
        assertFalse(token.isExpired());
        assertFalse(token.isUsable());
    }
}

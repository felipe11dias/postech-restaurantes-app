package com.postech.restaurantes.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    @DisplayName("token emitido nasce sem id e guarda o usuário, o hash e a expiração")
    void emitidoGuardaOsDados() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken token = PasswordResetToken.issue(userId, "hash", expiracao);

        assertNull(token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(expiracao, token.getExpiresAt());
    }

    @Test
    @DisplayName("restore reconstrói um token já persistido, inclusive já usado")
    void restoreReconstroiOToken() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken token = PasswordResetToken.restore(id, userId, "hash", expiracao, true);

        assertEquals(id, token.getId());
        assertTrue(token.isUsed());
        assertFalse(token.isUsable());
    }

    @Test
    @DisplayName("o id é atribuído pelo adapter após o INSERT e não pode ser reatribuído")
    void assignId() {
        PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), "hash", LocalDateTime.now().plusMinutes(30));
        UUID id = UUID.randomUUID();

        token.assignId(id);

        assertEquals(id, token.getId());
        assertThrows(IllegalStateException.class, () -> token.assignId(UUID.randomUUID()));
    }

    /** Um token sem dono, sem hash ou sem prazo não teria como ser validado depois. */
    @Test
    @DisplayName("exige usuário, hash e expiração")
    void exigeCamposObrigatorios() {
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(30);

        assertThrows(IllegalArgumentException.class,
                () -> PasswordResetToken.issue(null, "hash", expiracao));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordResetToken.issue(UUID.randomUUID(), "  ", expiracao));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordResetToken.issue(UUID.randomUUID(), "hash", null));
    }
}

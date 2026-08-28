package com.postech.restaurantes.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetToken — token de redefinição de senha")
class PasswordResetTokenTest {

    private static final UUID TOKEN_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final UUID USER_ID = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd");

    @Test
    @DisplayName("builder preenche todos os campos")
    void builder_devePreencherTodosOsCampos() {
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(TOKEN_ID)
                .userId(USER_ID)
                .tokenHash("HASH")
                .expiresAt(expiracao)
                .used(true)
                .build();

        assertThat(token.getId()).isEqualTo(TOKEN_ID);
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getTokenHash()).isEqualTo("HASH");
        assertThat(token.getExpiresAt()).isEqualTo(expiracao);
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("setters alteram o token criado pelo construtor sem argumentos")
    void setters_deveAlterarCampos() {
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(15);
        PasswordResetToken token = new PasswordResetToken();

        token.setId(TOKEN_ID);
        token.setUserId(USER_ID);
        token.setTokenHash("OUTRO_HASH");
        token.setExpiresAt(expiracao);
        token.setUsed(false);

        assertThat(token.getId()).isEqualTo(TOKEN_ID);
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getTokenHash()).isEqualTo("OUTRO_HASH");
        assertThat(token.getExpiresAt()).isEqualTo(expiracao);
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    @DisplayName("isExpired é falso enquanto a expiração está no futuro")
    void isExpired_comExpiracaoNoFuturo_deveSerFalso() {
        PasswordResetToken token = new PasswordResetToken(TOKEN_ID, USER_ID, "HASH",
                LocalDateTime.now().plusMinutes(5), false);

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("isExpired é verdadeiro quando a expiração já passou")
    void isExpired_comExpiracaoNoPassado_deveSerVerdadeiro() {
        PasswordResetToken token = new PasswordResetToken(TOKEN_ID, USER_ID, "HASH",
                LocalDateTime.now().minusMinutes(1), false);

        assertThat(token.isExpired()).isTrue();
    }
}

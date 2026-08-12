package com.postech.restaurantes.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de redefinição de senha: uso único, com expiração.
 * Referencia o usuário pelo id, mesmo motivo descrito em {@link Address}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken extends Auditable {

    private UUID id;
    private UUID userId;

    /** Hash SHA-256 do token bruto — o token em claro nunca é persistido. */
    private String tokenHash;

    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean used = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

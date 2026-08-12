package com.postech.restaurantes.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de redefinição de senha: uso único, com expiração.
 * Referencia o usuário pelo id, mesmo motivo descrito em {@link Address}.
 */
public class PasswordResetToken extends Auditable {

    private UUID id;
    private UUID userId;

    /** Hash SHA-256 do token bruto — o token em claro nunca é persistido. */
    private String tokenHash;

    private LocalDateTime expiresAt;
    private boolean used;

    public PasswordResetToken() {
    }

    public PasswordResetToken(UUID id, UUID userId, String tokenHash,
                              LocalDateTime expiresAt, boolean used) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private UUID id;
        private UUID userId;
        private String tokenHash;
        private LocalDateTime expiresAt;
        private boolean used;

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder used(boolean used) {
            this.used = used;
            return this;
        }

        public PasswordResetToken build() {
            return new PasswordResetToken(id, userId, tokenHash, expiresAt, used);
        }
    }
}

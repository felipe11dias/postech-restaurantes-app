package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de redefinição de senha: uso único, com expiração.
 *
 * <p>Guarda apenas o <strong>hash</strong> do token — o valor em claro existe só
 * durante o envio do e-mail e nunca é persistido. As duas regras que decidem se um
 * token ainda vale (expirou? já foi usado?) são de negócio e moram aqui, não no
 * serviço que as consulta.</p>
 *
 * <p>Referencia o usuário pelo id em vez de pelo objeto: o token não faz parte do
 * agregado {@link User}, tem ciclo de vida próprio, e carregar o usuário inteiro
 * para validar um token seria trabalho desperdiçado.</p>
 */
public class PasswordResetToken {

    private UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private boolean used;

    private PasswordResetToken(UUID id, UUID userId, String tokenHash,
                               LocalDateTime expiresAt, boolean used) {
        this.id = id;
        this.userId = ObjectUtils.requireNonNull(userId, "O usuário do token é obrigatório");
        this.tokenHash = ObjectUtils.requireNonBlank(tokenHash, "O hash do token é obrigatório");
        this.expiresAt = ObjectUtils.requireNonNull(expiresAt, "A expiração do token é obrigatória");
        this.used = used;
    }

    /** Emite um token novo, ainda não utilizado. */
    public static PasswordResetToken issue(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        return new PasswordResetToken(null, userId, tokenHash, expiresAt, false);
    }

    /** Reconstrói um token já persistido. */
    public static PasswordResetToken restore(UUID id, UUID userId, String tokenHash,
                                             LocalDateTime expiresAt, boolean used) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, used);
    }

    public UUID getId() {
        return id;
    }

    /** Atribuído uma única vez, pelo adapter de persistência, após o INSERT. */
    public void assignId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("O id do token já foi atribuído");
        }
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Um token só pode ser gasto uma vez; depois disso ele deixa de ser utilizável. */
    public boolean isUsable() {
        return !used && !isExpired();
    }

    public void markUsed() {
        this.used = true;
    }
}

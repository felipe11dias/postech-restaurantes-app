package com.postech.restaurantes.repository;

import com.postech.restaurantes.config.AuditorProvider;
import com.postech.restaurantes.entity.PasswordResetToken;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em JDBC do repositório de tokens de redefinição de senha.
 */
@Repository
public class PasswordResetTokenJdbc implements PasswordResetTokenRepository {

    private static final String COLUNAS =
            "id, user_id, token_hash, expires_at, used, created_at, last_updated_at, created_by, last_updated_by";

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditorProvider auditorProvider;

    public PasswordResetTokenJdbc(NamedParameterJdbcTemplate jdbc, AuditorProvider auditorProvider) {
        this.jdbc = jdbc;
        this.auditorProvider = auditorProvider;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jdbc.query(
                "SELECT " + COLUNAS + " FROM password_reset_tokens WHERE token_hash = :tokenHash",
                new MapSqlParameterSource("tokenHash", tokenHash), this::mapearToken)
                .stream()
                .findFirst();
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        String auditor = auditorProvider.currentAuditor();
        LocalDateTime agora = LocalDateTime.now();

        if (token.getId() == null) {
            token.markCreated(auditor, agora);
            token.setId(inserir(token));
        } else {
            token.markUpdated(auditor, agora);
            atualizar(token);
        }
        return token;
    }

    private UUID inserir(PasswordResetToken token) {
        return jdbc.queryForObject("""
                INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, used,
                                                   created_at, last_updated_at, created_by, last_updated_by)
                VALUES (:userId, :tokenHash, :expiresAt, :used,
                        :createdAt, :lastUpdatedAt, :createdBy, :lastUpdatedBy)
                RETURNING id
                """, parametrosDe(token), UUID.class);
    }

    private void atualizar(PasswordResetToken token) {
        jdbc.update("""
                UPDATE password_reset_tokens
                   SET user_id = :userId, token_hash = :tokenHash, expires_at = :expiresAt, used = :used,
                       last_updated_at = :lastUpdatedAt, last_updated_by = :lastUpdatedBy
                 WHERE id = :id
                """, parametrosDe(token).addValue("id", token.getId()));
    }

    private MapSqlParameterSource parametrosDe(PasswordResetToken token) {
        return new MapSqlParameterSource()
                .addValue("userId", token.getUserId())
                .addValue("tokenHash", token.getTokenHash())
                .addValue("expiresAt", token.getExpiresAt())
                .addValue("used", token.isUsed())
                .addValue("createdAt", token.getCreatedAt())
                .addValue("lastUpdatedAt", token.getLastUpdatedAt())
                .addValue("createdBy", token.getCreatedBy())
                .addValue("lastUpdatedBy", token.getLastUpdatedBy());
    }

    private PasswordResetToken mapearToken(ResultSet rs, int linha) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(rs.getObject("id", UUID.class));
        token.setUserId(rs.getObject("user_id", UUID.class));
        token.setTokenHash(rs.getString("token_hash"));
        token.setExpiresAt(rs.getObject("expires_at", LocalDateTime.class));
        token.setUsed(rs.getBoolean("used"));
        token.restoreAudit(
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("last_updated_at", LocalDateTime.class),
                rs.getString("created_by"),
                rs.getString("last_updated_by"));
        return token;
    }
}

package com.postech.restaurantes.adapter.out.persistence;

import com.postech.restaurantes.application.port.out.AuditorPort;
import com.postech.restaurantes.application.port.out.LoadPasswordResetTokenPort;
import com.postech.restaurantes.application.port.out.SavePasswordResetTokenPort;
import com.postech.restaurantes.domain.model.PasswordResetToken;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Adapter de saída de persistência dos tokens de redefinição de senha. */
@Component
public class PasswordResetTokenPersistenceAdapter
        implements LoadPasswordResetTokenPort, SavePasswordResetTokenPort {

    private static final String COLUNAS = "id, user_id, token_hash, expires_at, used";

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditorPort auditorPort;

    public PasswordResetTokenPersistenceAdapter(NamedParameterJdbcTemplate jdbc,
                                                AuditorPort auditorPort) {
        this.jdbc = jdbc;
        this.auditorPort = auditorPort;
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
        String auditor = auditorPort.currentAuditor();
        LocalDateTime agora = LocalDateTime.now();

        if (token.getId() == null) {
            token.assignId(inserir(token, auditor, agora));
        } else {
            atualizar(token, auditor, agora);
        }
        return token;
    }

    private UUID inserir(PasswordResetToken token, String auditor, LocalDateTime agora) {
        return jdbc.queryForObject("""
                INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, used,
                                                   created_at, last_updated_at,
                                                   created_by, last_updated_by)
                VALUES (:userId, :tokenHash, :expiresAt, :used,
                        :agora, :agora, :auditor, :auditor)
                RETURNING id
                """, parametrosDe(token, auditor, agora), UUID.class);
    }

    private void atualizar(PasswordResetToken token, String auditor, LocalDateTime agora) {
        jdbc.update("""
                UPDATE password_reset_tokens
                   SET used = :used, last_updated_at = :agora, last_updated_by = :auditor
                 WHERE id = :id
                """, parametrosDe(token, auditor, agora).addValue("id", token.getId()));
    }

    private MapSqlParameterSource parametrosDe(PasswordResetToken token, String auditor,
                                               LocalDateTime agora) {
        return new MapSqlParameterSource()
                .addValue("userId", token.getUserId())
                .addValue("tokenHash", token.getTokenHash())
                .addValue("expiresAt", token.getExpiresAt())
                .addValue("used", token.isUsed())
                .addValue("auditor", auditor)
                .addValue("agora", agora);
    }

    private PasswordResetToken mapearToken(ResultSet rs, int linha) throws SQLException {
        return PasswordResetToken.restore(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("token_hash"),
                rs.getObject("expires_at", LocalDateTime.class),
                rs.getBoolean("used"));
    }
}

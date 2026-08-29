package com.postech.restaurantes.adapter.out.persistence;

import com.postech.restaurantes.application.port.out.AuditorPort;
import com.postech.restaurantes.domain.model.PasswordResetToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Sem ORM, o SQL e o mapeamento do ResultSet são código próprio — e é isso que
 * estes testes exercitam, alimentando o RowMapper capturado na chamada com um
 * ResultSet dublado.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetTokenPersistenceAdapterTest {

    private static final UUID TOKEN_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDateTime EXPIRACAO = LocalDateTime.now().plusMinutes(30);

    @Mock
    private NamedParameterJdbcTemplate jdbc;
    @Mock
    private AuditorPort auditorPort;

    @InjectMocks
    private PasswordResetTokenPersistenceAdapter adapter;

    @Test
    @DisplayName("busca pelo hash devolve o token encontrado")
    void buscaPeloHash() {
        PasswordResetToken encontrado =
                PasswordResetToken.restore(TOKEN_ID, USER_ID, "hash", EXPIRACAO, false);
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(encontrado));

        assertEquals(Optional.of(encontrado), adapter.findByTokenHash("hash"));
    }

    @Test
    @DisplayName("busca pelo hash devolve vazio quando o token não existe")
    void buscaSemResultado() {
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        assertTrue(adapter.findByTokenHash("inexistente").isEmpty());
    }

    @Test
    @DisplayName("token sem id é inserido e recebe o id gerado")
    void tokenNovoEhInserido() {
        given(auditorPort.currentAuditor()).willReturn("system");
        given(jdbc.queryForObject(contains("INSERT INTO password_reset_tokens"),
                any(SqlParameterSource.class), eq(UUID.class))).willReturn(TOKEN_ID);

        PasswordResetToken salvo =
                adapter.save(PasswordResetToken.issue(USER_ID, "hash", EXPIRACAO));

        assertEquals(TOKEN_ID, salvo.getId());
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("a inserção leva os dados do token e o auditor para o SQL")
    void insercaoLevaOsParametros() {
        given(auditorPort.currentAuditor()).willReturn("system");
        given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(UUID.class)))
                .willReturn(TOKEN_ID);

        adapter.save(PasswordResetToken.issue(USER_ID, "hash", EXPIRACAO));

        ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForObject(anyString(), captor.capture(), eq(UUID.class));
        assertEquals(USER_ID, captor.getValue().getValue("userId"));
        assertEquals("hash", captor.getValue().getValue("tokenHash"));
        assertEquals(EXPIRACAO, captor.getValue().getValue("expiresAt"));
        assertEquals(false, captor.getValue().getValue("used"));
        assertEquals("system", captor.getValue().getValue("auditor"));
    }

    /** Gastar o token é um UPDATE: o registro já existe. */
    @Test
    @DisplayName("token com id é atualizado, marcando o consumo")
    void tokenExistenteEhAtualizado() {
        given(auditorPort.currentAuditor()).willReturn("maria.silva");
        PasswordResetToken token =
                PasswordResetToken.restore(TOKEN_ID, USER_ID, "hash", EXPIRACAO, false);
        token.markUsed();

        adapter.save(token);

        ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(contains("UPDATE password_reset_tokens"), captor.capture());
        assertEquals(TOKEN_ID, captor.getValue().getValue("id"));
        assertEquals(true, captor.getValue().getValue("used"));
        verify(jdbc, never()).queryForObject(anyString(), any(SqlParameterSource.class), eq(UUID.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("o mapeamento reconstrói o token a partir do ResultSet")
    void mapeamentoDoResultSet() throws Exception {
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());
        adapter.findByTokenHash("hash");

        ArgumentCaptor<RowMapper<PasswordResetToken>> captor =
                ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(anyString(), any(SqlParameterSource.class), captor.capture());

        ResultSet rs = mock(ResultSet.class);
        given(rs.getObject("id", UUID.class)).willReturn(TOKEN_ID);
        given(rs.getObject("user_id", UUID.class)).willReturn(USER_ID);
        given(rs.getString("token_hash")).willReturn("hash");
        given(rs.getObject("expires_at", LocalDateTime.class)).willReturn(EXPIRACAO);
        given(rs.getBoolean("used")).willReturn(false);

        PasswordResetToken token = captor.getValue().mapRow(rs, 1);

        assertEquals(TOKEN_ID, token.getId());
        assertEquals(USER_ID, token.getUserId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(EXPIRACAO, token.getExpiresAt());
        assertFalse(token.isUsed());
    }
}

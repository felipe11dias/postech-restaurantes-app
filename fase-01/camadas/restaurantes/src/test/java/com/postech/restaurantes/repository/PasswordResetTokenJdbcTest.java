package com.postech.restaurantes.repository;

import com.postech.restaurantes.config.AuditorProvider;
import com.postech.restaurantes.entity.PasswordResetToken;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenJdbc — persistência do token em SQL")
class PasswordResetTokenJdbcTest {

    private static final UUID TOKEN_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final LocalDateTime EXPIRACAO = LocalDateTime.of(2026, 3, 1, 12, 0);

    @Mock private NamedParameterJdbcTemplate jdbc;
    @Mock private AuditorProvider auditorProvider;

    @InjectMocks private PasswordResetTokenJdbc repository;

    private PasswordResetToken token(UUID id) {
        return PasswordResetToken.builder()
                .id(id)
                .userId(USER_ID)
                .tokenHash("HASH-DO-TOKEN")
                .expiresAt(EXPIRACAO)
                .used(false)
                .build();
    }

    private SqlParameterSource capturarParametrosDoInsert() {
        ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForObject(anyString(), captor.capture(), eq(UUID.class));
        return captor.getValue();
    }

    @Test
    @DisplayName("findByTokenHash devolve o token encontrado")
    void findByTokenHash_deveDevolverOToken() {
        PasswordResetToken encontrado = token(TOKEN_ID);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(encontrado));

        Optional<PasswordResetToken> resultado = repository.findByTokenHash("HASH-DO-TOKEN");

        assertThat(resultado).containsSame(encontrado);
    }

    @Test
    @DisplayName("findByTokenHash devolve vazio quando o hash não existe")
    void findByTokenHash_semResultado_deveDevolverVazio() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        assertThat(repository.findByTokenHash("INEXISTENTE")).isEmpty();
    }

    @Test
    @DisplayName("token sem id é inserido, recebendo o id gerado e as marcas de criação")
    void save_semId_deveInserir() {
        when(auditorProvider.currentAuditor()).thenReturn("system");
        when(jdbc.queryForObject(contains("INSERT INTO password_reset_tokens"),
                any(SqlParameterSource.class), eq(UUID.class))).thenReturn(TOKEN_ID);

        PasswordResetToken salvo = repository.save(token(null));

        assertThat(salvo.getId()).isEqualTo(TOKEN_ID);
        assertThat(salvo.getCreatedBy()).isEqualTo("system");
        assertThat(salvo.getLastUpdatedBy()).isEqualTo("system");
        assertThat(salvo.getCreatedAt()).isNotNull().isEqualTo(salvo.getLastUpdatedAt());
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("a inserção leva todos os campos do token para o SQL")
    void save_semId_deveEnviarTodosOsParametros() {
        when(auditorProvider.currentAuditor()).thenReturn("system");
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(UUID.class)))
                .thenReturn(TOKEN_ID);

        repository.save(token(null));

        SqlParameterSource parametros = capturarParametrosDoInsert();
        assertThat(parametros.getValue("userId")).isEqualTo(USER_ID);
        assertThat(parametros.getValue("tokenHash")).isEqualTo("HASH-DO-TOKEN");
        assertThat(parametros.getValue("expiresAt")).isEqualTo(EXPIRACAO);
        assertThat(parametros.getValue("used")).isEqualTo(false);
        assertThat(parametros.getValue("createdBy")).isEqualTo("system");
    }

    /** Marcar o token como usado é um UPDATE — o registro já existe. */
    @Test
    @DisplayName("token com id é atualizado, preservando quem o criou")
    void save_comId_deveAtualizar() {
        PasswordResetToken existente = token(TOKEN_ID);
        existente.restoreAudit(LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0), "system", "system");
        existente.setUsed(true);
        when(auditorProvider.currentAuditor()).thenReturn("joao.silva");

        repository.save(existente);

        assertThat(existente.getCreatedBy()).isEqualTo("system");
        assertThat(existente.getLastUpdatedBy()).isEqualTo("joao.silva");

        ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(contains("UPDATE password_reset_tokens"), captor.capture());
        assertThat(captor.getValue().getValue("id")).isEqualTo(TOKEN_ID);
        assertThat(captor.getValue().getValue("used")).isEqualTo(true);
        verify(jdbc, never()).queryForObject(anyString(), any(SqlParameterSource.class), eq(UUID.class));
    }

    @Test
    @DisplayName("o mapeamento do ResultSet reconstrói o token e a auditoria")
    @SuppressWarnings("unchecked")
    void rowMapper_deveMapearOTokenCompleto() throws Exception {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        repository.findByTokenHash("HASH-DO-TOKEN");

        ArgumentCaptor<RowMapper<PasswordResetToken>> captor =
                ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(anyString(), any(SqlParameterSource.class), captor.capture());

        LocalDateTime criacao = LocalDateTime.of(2026, 1, 10, 8, 0);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id", UUID.class)).thenReturn(TOKEN_ID);
        when(rs.getObject("user_id", UUID.class)).thenReturn(USER_ID);
        when(rs.getString("token_hash")).thenReturn("HASH-DO-TOKEN");
        when(rs.getObject("expires_at", LocalDateTime.class)).thenReturn(EXPIRACAO);
        when(rs.getBoolean("used")).thenReturn(true);
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(criacao);
        when(rs.getObject("last_updated_at", LocalDateTime.class)).thenReturn(criacao);
        when(rs.getString("created_by")).thenReturn("system");
        when(rs.getString("last_updated_by")).thenReturn("joao.silva");

        PasswordResetToken token = captor.getValue().mapRow(rs, 1);

        assertThat(token.getId()).isEqualTo(TOKEN_ID);
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getTokenHash()).isEqualTo("HASH-DO-TOKEN");
        assertThat(token.getExpiresAt()).isEqualTo(EXPIRACAO);
        assertThat(token.isUsed()).isTrue();
        assertThat(token.getCreatedAt()).isEqualTo(criacao);
        assertThat(token.getCreatedBy()).isEqualTo("system");
        assertThat(token.getLastUpdatedBy()).isEqualTo("joao.silva");
    }
}

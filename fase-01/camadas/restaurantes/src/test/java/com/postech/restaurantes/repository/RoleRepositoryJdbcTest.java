package com.postech.restaurantes.repository;

import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.enums.RoleName;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sem ORM, o mapeamento do ResultSet para a entidade é código próprio — e é
 * justamente o que estes testes exercitam, alimentando o RowMapper capturado com
 * um ResultSet simulado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleRepositoryJdbc — consulta de papéis em SQL")
class RoleRepositoryJdbcTest {

    private static final UUID ROLE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 1, 10, 8, 0);

    @Mock private NamedParameterJdbcTemplate jdbc;

    @InjectMocks private RoleRepositoryJdbc repository;

    @SuppressWarnings("unchecked")
    private RowMapper<Role> capturarRowMapper() {
        ArgumentCaptor<RowMapper<Role>> captor = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(anyString(), any(SqlParameterSource.class), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("findByName devolve o papel encontrado")
    void findByName_deveDevolverOPapel() {
        Role encontrado = new Role(ROLE_ID, RoleName.ROLE_ADMIN);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(encontrado));

        Optional<Role> resultado = repository.findByName(RoleName.ROLE_ADMIN);

        assertThat(resultado).containsSame(encontrado);
    }

    @Test
    @DisplayName("findByName devolve vazio quando o papel não existe")
    void findByName_semResultado_deveDevolverVazio() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        assertThat(repository.findByName(RoleName.ROLE_OWNER)).isEmpty();
    }

    @Test
    @DisplayName("consulta filtra pelo nome do papel como texto")
    void findByName_deveFiltrarPeloNomeComoTexto() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        repository.findByName(RoleName.ROLE_CUSTOMER);

        ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(anyString(), captor.capture(), any(RowMapper.class));
        assertThat(captor.getValue().getValue("name")).isEqualTo("ROLE_CUSTOMER");
    }

    /** A coluna guarda o nome do papel como texto: a volta para o enum é explícita. */
    @Test
    @DisplayName("o mapeamento converte a coluna de texto de volta para o enum e repõe a auditoria")
    void rowMapper_deveMapearOPapelCompleto() throws Exception {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        repository.findByName(RoleName.ROLE_ADMIN);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id", UUID.class)).thenReturn(ROLE_ID);
        when(rs.getString("name")).thenReturn("ROLE_ADMIN");
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(CRIACAO);
        when(rs.getObject("last_updated_at", LocalDateTime.class)).thenReturn(CRIACAO);
        when(rs.getString("created_by")).thenReturn("system");
        when(rs.getString("last_updated_by")).thenReturn("system");

        Role papel = capturarRowMapper().mapRow(rs, 1);

        assertThat(papel.getId()).isEqualTo(ROLE_ID);
        assertThat(papel.getName()).isEqualTo(RoleName.ROLE_ADMIN);
        assertThat(papel.getCreatedAt()).isEqualTo(CRIACAO);
        assertThat(papel.getLastUpdatedAt()).isEqualTo(CRIACAO);
        assertThat(papel.getCreatedBy()).isEqualTo("system");
        assertThat(papel.getLastUpdatedBy()).isEqualTo("system");
    }
}

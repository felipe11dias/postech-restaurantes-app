package com.postech.restaurantes.adapter.out.persistence;

import com.postech.restaurantes.domain.model.Role;
import com.postech.restaurantes.domain.model.RoleName;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RolePersistenceAdapterTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private RolePersistenceAdapter adapter;

    @Test
    @DisplayName("devolve o papel encontrado")
    void devolveOPapel() {
        Role encontrado = Role.restore(UUID.randomUUID(), RoleName.ROLE_ADMIN);
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(encontrado));

        assertEquals(encontrado, adapter.findByName(RoleName.ROLE_ADMIN).orElseThrow());
    }

    @Test
    @DisplayName("devolve vazio quando o papel não existe")
    void semResultado() {
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        assertTrue(adapter.findByName(RoleName.ROLE_OWNER).isEmpty());
    }

    @Test
    @DisplayName("filtra pelo nome do papel como texto")
    void filtraPeloNomeComoTexto() {
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());

        adapter.findByName(RoleName.ROLE_CUSTOMER);

        ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(anyString(), captor.capture(), any(RowMapper.class));
        assertEquals("ROLE_CUSTOMER", captor.getValue().getValue("name"));
    }

    /** A coluna guarda o nome como texto: a volta para o enum é explícita. */
    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("o mapeamento converte a coluna de texto de volta para o enum")
    void mapeamentoConverteParaEnum() throws Exception {
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());
        adapter.findByName(RoleName.ROLE_ADMIN);

        ArgumentCaptor<RowMapper<Role>> captor = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(anyString(), any(SqlParameterSource.class), captor.capture());

        UUID id = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        given(rs.getObject("id", UUID.class)).willReturn(id);
        given(rs.getString("name")).willReturn("ROLE_ADMIN");

        Role papel = captor.getValue().mapRow(rs, 1);

        assertEquals(id, papel.getId());
        assertEquals(RoleName.ROLE_ADMIN, papel.getName());
    }
}

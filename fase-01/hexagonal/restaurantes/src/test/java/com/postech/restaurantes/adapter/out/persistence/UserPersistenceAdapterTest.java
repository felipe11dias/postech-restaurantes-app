package com.postech.restaurantes.adapter.out.persistence;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageQuery.SortDirection;
import com.postech.restaurantes.application.pagination.PageResult;
import com.postech.restaurantes.application.port.out.AuditorPort;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.model.Address;
import com.postech.restaurantes.domain.model.RoleName;
import com.postech.restaurantes.domain.model.User;
import com.postech.restaurantes.domain.model.shared.Email;
import com.postech.restaurantes.domain.model.shared.ZipCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Este adapter é o único ponto do sistema que conhece tabelas e colunas: o
 * cascade, o orphanRemoval e a tabela associativa de papéis viraram SQL escrito à
 * mão. Os testes verificam esse SQL contra um NamedParameterJdbcTemplate dublado.
 *
 * <p>As linhas de resultado são produzidas pelo caminho real: o dublê invoca o
 * próprio RowMapper do adapter sobre um ResultSet simulado. Assim o mapeamento
 * entra no teste em vez de ser contornado por objetos montados à mão — o que
 * também seria impossível, já que a linha crua é um record privado do adapter.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserPersistenceAdapterTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 1, 10, 8, 0);

    @Mock
    private NamedParameterJdbcTemplate jdbc;
    @Mock
    private AuditorPort auditorPort;

    @InjectMocks
    private UserPersistenceAdapter adapter;

    private ResultSet linhaDeUsuario;

    @BeforeEach
    void montarLinhaDeUsuario() throws Exception {
        linhaDeUsuario = mock(ResultSet.class);
        given(linhaDeUsuario.getObject("id", UUID.class)).willReturn(USER_ID);
        given(linhaDeUsuario.getString("name")).willReturn("Maria Silva");
        given(linhaDeUsuario.getString("email")).willReturn("maria@email.com");
        given(linhaDeUsuario.getString("login")).willReturn("maria.silva");
        given(linhaDeUsuario.getString("password")).willReturn(DomainFixtures.SENHA_HASH);
        given(linhaDeUsuario.getObject("created_at", LocalDateTime.class)).willReturn(CRIACAO);
        given(linhaDeUsuario.getObject("last_updated_at", LocalDateTime.class)).willReturn(CRIACAO);
    }

    /** Faz a consulta devolver N linhas, passando o ResultSet pelo RowMapper real. */
    private void devolverLinhas(int quantidade) {
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willAnswer(chamada -> {
                    RowMapper<?> mapper = chamada.getArgument(2);
                    List<Object> linhas = new java.util.ArrayList<>();
                    for (int i = 0; i < quantidade; i++) {
                        linhas.add(mapper.mapRow(linhaDeUsuario, i + 1));
                    }
                    return linhas;
                });
    }

    private void semLinhas() {
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of());
    }

    private String sqlDoRowMapper() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(captor.capture(), any(SqlParameterSource.class), any(RowMapper.class));
        return captor.getValue();
    }

    @Nested
    @DisplayName("consultas de um único usuário")
    class ConsultaUnica {

        @Test
        @DisplayName("findById devolve o usuário mapeado, com o hash da senha")
        void findById() {
            devolverLinhas(1);

            User user = adapter.findById(USER_ID).orElseThrow();

            assertEquals(USER_ID, user.getId());
            assertEquals("Maria Silva", user.getName());
            assertEquals("maria@email.com", user.getEmail().value());
            assertEquals("maria.silva", user.getLogin());
            assertEquals(DomainFixtures.SENHA_HASH, user.getPassword());
            assertEquals(CRIACAO, user.getCreatedAt());
            assertEquals(CRIACAO, user.getLastUpdatedAt());
            assertTrue(sqlDoRowMapper().contains("id = :valor"));
        }

        @Test
        @DisplayName("findByLogin filtra pela coluna login")
        void findByLogin() {
            devolverLinhas(1);

            assertTrue(adapter.findByLogin("maria.silva").isPresent());
            assertTrue(sqlDoRowMapper().contains("login = :valor"));
        }

        @Test
        @DisplayName("findByEmail filtra pela coluna email")
        void findByEmail() {
            devolverLinhas(1);

            assertTrue(adapter.findByEmail("maria@email.com").isPresent());
            assertTrue(sqlDoRowMapper().contains("email = :valor"));
        }

        /** Sem linhas não há associações a carregar: as consultas extras nem acontecem. */
        @Test
        @DisplayName("consulta sem resultado devolve vazio e não busca associações")
        void semResultado() {
            semLinhas();

            assertTrue(adapter.findById(USER_ID).isEmpty());
            verify(jdbc, never())
                    .query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        }

        /** Agrupar por id é o que impede o mesmo usuário de aparecer duas vezes. */
        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("linha repetida é agrupada uma única vez")
        void linhaRepetida() {
            devolverLinhas(2);

            assertTrue(adapter.findById(USER_ID).isPresent());

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc, times(2))
                    .query(anyString(), captor.capture(), any(RowCallbackHandler.class));
            Collection<UUID> ids = (Collection<UUID>) captor.getValue().getValue("userIds");
            assertEquals(1, ids.size());
        }
    }

    @Nested
    @DisplayName("checagens de existência")
    class Existencia {

        @Test
        @DisplayName("existsById reflete a resposta do banco")
        void existsById() {
            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .willReturn(true);
            assertTrue(adapter.existsById(USER_ID));

            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .willReturn(false);
            assertFalse(adapter.existsById(USER_ID));
        }

        @Test
        @DisplayName("resposta nula do banco é tratada como inexistente")
        void respostaNula() {
            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .willReturn(null);

            assertFalse(adapter.existsById(USER_ID));
        }

        /**
         * No cadastro não há registro a excluir da checagem — daí o IS NULL, já que
         * {@code <> NULL} nunca é verdadeiro em SQL.
         */
        @Test
        @DisplayName("no cadastro, nenhum registro é excluído da checagem de e-mail")
        void emailNoCadastro() {
            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .willReturn(false);

            assertFalse(adapter.existsByEmailExcluding("maria@email.com", null));

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).queryForObject(sql.capture(), params.capture(), eq(Boolean.class));
            assertTrue(sql.getValue().contains("email = :valor"));
            assertTrue(sql.getValue().contains("CAST(:atual AS uuid) IS NULL"));
            assertEquals("maria@email.com", params.getValue().getValue("valor"));
            assertNull(params.getValue().getValue("atual"));
        }

        @Test
        @DisplayName("na atualização, o próprio usuário fica de fora da checagem de login")
        void loginNaAtualizacao() {
            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .willReturn(true);

            assertTrue(adapter.existsByLoginExcluding("maria.silva", USER_ID));

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).queryForObject(sql.capture(), params.capture(), eq(Boolean.class));
            assertTrue(sql.getValue().contains("login = :valor"));
            assertEquals(USER_ID, params.getValue().getValue("atual"));
        }
    }

    @Nested
    @DisplayName("consultas paginadas")
    class Paginacao {

        private void totalDe(Long total) {
            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                    .willReturn(total);
        }

        @Test
        @DisplayName("sem usuários devolve página vazia, sem consultar as linhas")
        void semUsuarios() {
            totalDe(0L);

            PageResult<User> pagina = adapter.findAll(PageQuery.of(0, 20));

            assertTrue(pagina.content().isEmpty());
            assertEquals(0L, pagina.totalElements());
            verify(jdbc, never()).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
        }

        @Test
        @DisplayName("contagem nula também resulta em página vazia")
        void contagemNula() {
            totalDe(null);

            assertTrue(adapter.findAll(PageQuery.of(0, 20)).content().isEmpty());
        }

        /** A listagem não carrega o hash de senha de todos os usuários da página. */
        @Test
        @DisplayName("a página traz os usuários e o total, sem o hash da senha")
        void paginaPreenchida() {
            totalDe(3L);
            devolverLinhas(1);

            PageResult<User> pagina = adapter.findAll(PageQuery.of(0, 20));

            assertEquals(1, pagina.content().size());
            assertEquals(3L, pagina.totalElements());
            assertNull(pagina.content().get(0).getPassword());
            assertTrue(sqlDoRowMapper().contains("LIMIT :limite OFFSET :deslocamento"));
            assertFalse(sqlDoRowMapper().contains(", password"));
        }

        @Test
        @DisplayName("limite e deslocamento vêm do PageQuery")
        void limiteEDeslocamento() {
            totalDe(30L);
            devolverLinhas(1);

            adapter.findAll(new PageQuery(2, 10, null, SortDirection.ASC));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).query(anyString(), captor.capture(), any(RowMapper.class));
            assertEquals(10, captor.getValue().getValue("limite"));
            assertEquals(20L, captor.getValue().getValue("deslocamento"));
        }

        @Test
        @DisplayName("busca por nome usa LIKE sem diferenciar maiúsculas de minúsculas")
        void buscaPorNome() {
            totalDe(1L);
            devolverLinhas(1);

            adapter.findByNameContaining("Ma", PageQuery.of(0, 20));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).query(anyString(), captor.capture(), any(RowMapper.class));
            assertEquals("%Ma%", captor.getValue().getValue("padrao"));
            assertTrue(sqlDoRowMapper().contains("LOWER(name) LIKE LOWER(:padrao)"));
        }

        /**
         * O mapa de colunas permitidas é imutável e recusa chave nula, então uma
         * consulta sem ordenação declarada precisa ser desviada antes da busca —
         * caso contrário a listagem inteira falharia com NullPointerException.
         */
        @Test
        @DisplayName("sem ordenação declarada, ordena por nome")
        void ordenacaoPadrao() {
            totalDe(1L);
            devolverLinhas(1);

            adapter.findAll(PageQuery.of(0, 20));

            assertTrue(sqlDoRowMapper().contains("ORDER BY name ASC"));
        }

        @Test
        @DisplayName("propriedade conhecida em ordem decrescente vira a coluna correspondente")
        void ordenacaoDecrescente() {
            totalDe(1L);
            devolverLinhas(1);

            adapter.findAll(new PageQuery(0, 20, "createdAt", SortDirection.DESC));

            assertTrue(sqlDoRowMapper().contains("ORDER BY created_at DESC"));
        }

        @Test
        @DisplayName("propriedade conhecida em ordem crescente é respeitada")
        void ordenacaoCrescente() {
            totalDe(1L);
            devolverLinhas(1);

            adapter.findAll(new PageQuery(0, 20, "email", SortDirection.ASC));

            assertTrue(sqlDoRowMapper().contains("ORDER BY email ASC"));
        }

        /**
         * O nome da coluna entra no ORDER BY por concatenação — não há como
         * parametrizá-lo. Só passam as propriedades da lista permitida; qualquer
         * outra é descartada e a ordenação cai no padrão.
         */
        @Test
        @DisplayName("propriedade fora da lista permitida é ignorada")
        void ordenacaoDesconhecida() {
            totalDe(1L);
            devolverLinhas(1);

            adapter.findAll(new PageQuery(0, 20, "password", SortDirection.ASC));

            assertTrue(sqlDoRowMapper().contains("ORDER BY name ASC"));
            assertFalse(sqlDoRowMapper().contains("ORDER BY password"));
        }
    }

    @Nested
    @DisplayName("gravação do agregado")
    class Gravacao {

        private User usuarioNovo() {
            return User.newUser("Maria Silva", new Email("maria@email.com"), "maria.silva",
                    DomainFixtures.SENHA_HASH, Set.of(DomainFixtures.roleCustomer()), List.of());
        }

        private User usuarioPersistido(List<Address> enderecos) {
            return User.restore(USER_ID, "Maria Silva", new Email("maria@email.com"), "maria.silva",
                    DomainFixtures.SENHA_HASH, Set.of(), enderecos, CRIACAO, CRIACAO);
        }

        @Test
        @DisplayName("usuário sem id é inserido e recebe o id gerado")
        void usuarioNovoEhInserido() {
            given(auditorPort.currentAuditor()).willReturn("system");
            given(jdbc.queryForObject(contains("INSERT INTO users"),
                    any(SqlParameterSource.class), eq(UUID.class))).willReturn(USER_ID);

            assertEquals(USER_ID, adapter.save(usuarioNovo()).getId());
            verify(jdbc, never()).update(contains("UPDATE users"), any(SqlParameterSource.class));
        }

        /** As datas do usuário vêm do domínio; o auditor é metadado da gravação. */
        @Test
        @DisplayName("a inserção leva os dados, as datas do domínio e o auditor")
        void insercaoLevaOsParametros() {
            given(auditorPort.currentAuditor()).willReturn("system");
            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(UUID.class)))
                    .willReturn(USER_ID);
            User user = usuarioNovo();

            adapter.save(user);

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).queryForObject(contains("INSERT INTO users"), captor.capture(), eq(UUID.class));
            assertEquals("Maria Silva", captor.getValue().getValue("name"));
            assertEquals("maria@email.com", captor.getValue().getValue("email"));
            assertEquals("maria.silva", captor.getValue().getValue("login"));
            assertEquals(DomainFixtures.SENHA_HASH, captor.getValue().getValue("password"));
            assertEquals(user.getCreatedAt(), captor.getValue().getValue("createdAt"));
            assertEquals("system", captor.getValue().getValue("auditor"));
        }

        @Test
        @DisplayName("usuário com id é atualizado, sem reescrever quem o criou")
        void usuarioExistenteEhAtualizado() {
            given(auditorPort.currentAuditor()).willReturn("maria.silva");

            adapter.save(usuarioPersistido(List.of()));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(contains("UPDATE users"), captor.capture());
            assertEquals(USER_ID, captor.getValue().getValue("id"));
            assertEquals("maria.silva", captor.getValue().getValue("auditor"));
            verify(jdbc, never()).queryForObject(contains("INSERT INTO users"),
                    any(SqlParameterSource.class), eq(UUID.class));
        }

        @Test
        @DisplayName("os papéis são reescritos na tabela associativa")
        void papeisReescritos() {
            given(auditorPort.currentAuditor()).willReturn("system");
            given(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(UUID.class)))
                    .willReturn(USER_ID);

            adapter.save(usuarioNovo());

            verify(jdbc).update(contains("DELETE FROM user_roles"), any(SqlParameterSource.class));

            ArgumentCaptor<SqlParameterSource[]> captor =
                    ArgumentCaptor.forClass(SqlParameterSource[].class);
            verify(jdbc).batchUpdate(contains("INSERT INTO user_roles"), captor.capture());
            assertEquals(1, captor.getValue().length);
            assertEquals(USER_ID, captor.getValue()[0].getValue("userId"));
        }

        @Test
        @DisplayName("usuário sem papéis apenas apaga os vínculos existentes")
        void semPapeis() {
            given(auditorPort.currentAuditor()).willReturn("system");

            adapter.save(usuarioPersistido(List.of()));

            verify(jdbc).update(contains("DELETE FROM user_roles"), any(SqlParameterSource.class));
            verify(jdbc, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
        }

        @Test
        @DisplayName("endereço novo é inserido e recebe o id gerado")
        void enderecoNovo() {
            given(auditorPort.currentAuditor()).willReturn("system");
            given(jdbc.queryForObject(contains("INSERT INTO addresses"),
                    any(SqlParameterSource.class), eq(UUID.class))).willReturn(ADDRESS_ID);
            User user = usuarioPersistido(List.of(DomainFixtures.endereco()));

            adapter.save(user);

            assertEquals(ADDRESS_ID, user.getAddresses().get(0).getId());

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).queryForObject(contains("INSERT INTO addresses"), captor.capture(), eq(UUID.class));
            assertEquals(USER_ID, captor.getValue().getValue("userId"));
            assertEquals("Av. Paulista", captor.getValue().getValue("street"));
            assertEquals("01310200", captor.getValue().getValue("zipCode"));
        }

        @Test
        @DisplayName("endereço já existente é atualizado e os ausentes são removidos")
        void enderecoExistente() {
            given(auditorPort.currentAuditor()).willReturn("system");
            Address existente = Address.restore(ADDRESS_ID, "Av. Paulista", "1500", null,
                    "Bela Vista", "São Paulo", "SP", new ZipCode("01310-200"));

            adapter.save(usuarioPersistido(List.of(existente)));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(contains("id NOT IN (:ids)"), captor.capture());
            assertEquals(List.of(ADDRESS_ID), captor.getValue().getValue("ids"));
            verify(jdbc).update(contains("UPDATE addresses"), any(SqlParameterSource.class));
        }

        /** Equivalente ao orphanRemoval: sem endereços na entidade, apaga todos. */
        @Test
        @DisplayName("usuário sem endereços tem todos os endereços removidos")
        void semEnderecos() {
            given(auditorPort.currentAuditor()).willReturn("system");

            adapter.save(usuarioPersistido(List.of()));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(eq("DELETE FROM addresses WHERE user_id = :userId"), captor.capture());
            assertEquals(USER_ID, captor.getValue().getValue("userId"));
        }

        @Test
        @DisplayName("exclusão remove o usuário, deixando o cascade do schema cuidar do resto")
        void exclusao() {
            adapter.deleteById(USER_ID);

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(contains("DELETE FROM users"), captor.capture());
            assertEquals(USER_ID, captor.getValue().getValue("id"));
        }
    }

    /**
     * Papéis e endereços de todos os usuários da página são carregados em duas
     * consultas, e não uma por usuário — o N+1 que o fetch do ORM escondia. O dublê
     * invoca o callback durante a chamada, que é quando o adapter de fato o usa.
     */
    @Nested
    @DisplayName("carga das associações")
    class Associacoes {

        private void devolverPapel(ResultSet rs) {
            willAnswer(chamada -> {
                ((RowCallbackHandler) chamada.getArgument(2)).processRow(rs);
                return null;
            }).given(jdbc).query(contains("user_roles"), any(SqlParameterSource.class),
                    any(RowCallbackHandler.class));
        }

        private void devolverEndereco(ResultSet rs) {
            willAnswer(chamada -> {
                ((RowCallbackHandler) chamada.getArgument(2)).processRow(rs);
                return null;
            }).given(jdbc).query(contains("FROM addresses"), any(SqlParameterSource.class),
                    any(RowCallbackHandler.class));
        }

        @Test
        @DisplayName("os papéis carregados entram no usuário")
        void papeis() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            given(rs.getObject("owner_id", UUID.class)).willReturn(USER_ID);
            given(rs.getObject("id", UUID.class)).willReturn(ROLE_ID);
            given(rs.getString("name")).willReturn("ROLE_CUSTOMER");
            devolverLinhas(1);
            devolverPapel(rs);

            User user = adapter.findById(USER_ID).orElseThrow();

            assertTrue(user.hasRole(RoleName.ROLE_CUSTOMER));
            assertFalse(user.hasRole(RoleName.ROLE_ADMIN));
        }

        @Test
        @DisplayName("os endereços carregados entram no usuário")
        void enderecos() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            given(rs.getObject("id", UUID.class)).willReturn(ADDRESS_ID);
            given(rs.getObject("user_id", UUID.class)).willReturn(USER_ID);
            given(rs.getString("street")).willReturn("Av. Paulista");
            given(rs.getString("number")).willReturn("1500");
            given(rs.getString("complement")).willReturn("Apto 42");
            given(rs.getString("neighborhood")).willReturn("Bela Vista");
            given(rs.getString("city")).willReturn("São Paulo");
            given(rs.getString("state")).willReturn("SP");
            given(rs.getString("zip_code")).willReturn("01310200");
            devolverLinhas(1);
            devolverEndereco(rs);

            Address endereco = adapter.findById(USER_ID).orElseThrow().getAddresses().get(0);

            assertEquals(ADDRESS_ID, endereco.getId());
            assertEquals("Av. Paulista", endereco.getStreet());
            assertEquals("1500", endereco.getNumber());
            assertEquals("Apto 42", endereco.getComplement());
            assertEquals("Bela Vista", endereco.getNeighborhood());
            assertEquals("São Paulo", endereco.getCity());
            assertEquals("SP", endereco.getState());
            assertEquals("01310200", endereco.getZipCode().value());
        }
    }
}

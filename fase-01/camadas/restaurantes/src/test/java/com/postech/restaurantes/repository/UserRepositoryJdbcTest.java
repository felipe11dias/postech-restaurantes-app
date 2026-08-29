package com.postech.restaurantes.repository;

import com.postech.restaurantes.config.AuditorProvider;
import com.postech.restaurantes.entity.Address;
import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.enums.RoleName;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

/**
 * O repositório é a peça que substituiu o Spring Data JPA por SQL escrito à mão:
 * o cascade, o orphanRemoval, a tabela associativa de papéis, a auditoria e a
 * paginação passaram a ser código próprio. Estes testes verificam esse código
 * contra um NamedParameterJdbcTemplate simulado — inclusive os mapeamentos de
 * ResultSet, alimentados a partir dos mapeadores capturados nas chamadas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserRepositoryJdbc — persistência do agregado de usuário em SQL")
class UserRepositoryJdbcTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ROLE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID ADDRESS_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 1, 10, 8, 0);

    @Mock private NamedParameterJdbcTemplate jdbc;
    @Mock private AuditorProvider auditorProvider;

    @InjectMocks private UserRepositoryJdbc repository;

    private User usuario(UUID id) {
        return User.builder()
                .id(id)
                .name("João Silva")
                .email("joao@email.com")
                .login("joao.silva")
                .password("HASH")
                .build();
    }

    @SuppressWarnings("unchecked")
    private void devolverUsuarios(List<User> usuarios) {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn((List) usuarios);
    }

    private String sqlDaConsulta() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(captor.capture(), any(SqlParameterSource.class), any(RowMapper.class));
        return captor.getValue();
    }

    @Nested
    @DisplayName("consultas de um único usuário")
    class ConsultaUnica {

        @Test
        @DisplayName("findById devolve o usuário e carrega suas associações")
        void findById_deveDevolverOUsuario() {
            devolverUsuarios(List.of(usuario(USER_ID)));

            Optional<User> resultado = repository.findById(USER_ID);

            assertThat(resultado).isPresent();
            assertThat(sqlDaConsulta()).contains("id = :valor").contains("password");
            verify(jdbc, org.mockito.Mockito.times(2))
                    .query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        }

        @Test
        @DisplayName("findByLogin filtra pela coluna login")
        void findByLogin_deveFiltrarPeloLogin() {
            devolverUsuarios(List.of(usuario(USER_ID)));

            assertThat(repository.findByLogin("joao.silva")).isPresent();
            assertThat(sqlDaConsulta()).contains("login = :valor");
        }

        @Test
        @DisplayName("findByEmail filtra pela coluna email")
        void findByEmail_deveFiltrarPeloEmail() {
            devolverUsuarios(List.of(usuario(USER_ID)));

            assertThat(repository.findByEmail("joao@email.com")).isPresent();
            assertThat(sqlDaConsulta()).contains("email = :valor");
        }

        /**
         * O agrupamento por id é o que evita o N+1: se a consulta trouxer o mesmo
         * usuário mais de uma vez, ele entra uma única vez no mapa de associações.
         */
        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("usuário repetido no resultado é agrupado uma única vez")
        void findById_comUsuarioRepetido_deveAgruparUmaVez() {
            devolverUsuarios(List.of(usuario(USER_ID), usuario(USER_ID)));

            assertThat(repository.findById(USER_ID)).isPresent();

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc, org.mockito.Mockito.times(2))
                    .query(anyString(), captor.capture(), any(RowCallbackHandler.class));
            Collection<UUID> idsConsultados =
                    (Collection<UUID>) captor.getValue().getValue("userIds");
            assertThat(idsConsultados).containsExactly(USER_ID);
        }

        /** Sem usuários não há o que associar: as consultas extras nem acontecem. */
        @Test
        @DisplayName("consulta sem resultado devolve vazio e não busca associações")
        void findById_semResultado_deveDevolverVazio() {
            devolverUsuarios(List.of());

            assertThat(repository.findById(USER_ID)).isEmpty();
            verify(jdbc, never())
                    .query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        }
    }

    @Nested
    @DisplayName("existsById")
    class Existencia {

        @Test
        @DisplayName("devolve verdadeiro quando o banco confirma a existência")
        void existente_deveSerVerdadeiro() {
            when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .thenReturn(true);

            assertThat(repository.existsById(USER_ID)).isTrue();
        }

        @Test
        @DisplayName("devolve falso quando o banco nega a existência")
        void inexistente_deveSerFalso() {
            when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .thenReturn(false);

            assertThat(repository.existsById(USER_ID)).isFalse();
        }

        @Test
        @DisplayName("resposta nula do banco é tratada como inexistente")
        void respostaNula_deveSerFalso() {
            when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class)))
                    .thenReturn(null);

            assertThat(repository.existsById(USER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("consultas paginadas")
    class Paginacao {

        private void totalDe(Long total) {
            when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                    .thenReturn(total);
        }

        private String sqlDaPagina() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(jdbc).query(captor.capture(), any(SqlParameterSource.class), any(RowMapper.class));
            return captor.getValue();
        }

        @Test
        @DisplayName("página vazia quando não há usuários, sem consultar as linhas")
        void semUsuarios_deveDevolverPaginaVazia() {
            totalDe(0L);

            Page<User> pagina = repository.findAll(PageRequest.of(0, 20));

            assertThat(pagina.getContent()).isEmpty();
            assertThat(pagina.getTotalElements()).isZero();
            verify(jdbc, never()).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
        }

        @Test
        @DisplayName("contagem nula também resulta em página vazia")
        void contagemNula_deveDevolverPaginaVazia() {
            totalDe(null);

            assertThat(repository.findAll(PageRequest.of(0, 20)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("página traz os usuários e o total, sem carregar o hash da senha")
        void comUsuarios_deveDevolverPaginaPreenchida() {
            totalDe(3L);
            devolverUsuarios(List.of(usuario(USER_ID)));

            Page<User> pagina = repository.findAll(PageRequest.of(0, 1));

            assertThat(pagina.getContent()).hasSize(1);
            assertThat(pagina.getTotalElements()).isEqualTo(3L);
            assertThat(pagina.getTotalPages()).isEqualTo(3);
            assertThat(sqlDaPagina()).doesNotContain(", password").contains("LIMIT :limite OFFSET :deslocamento");
        }

        @Test
        @DisplayName("limite e deslocamento vêm do Pageable")
        void deveEnviarLimiteEDeslocamento() {
            totalDe(30L);
            devolverUsuarios(List.of(usuario(USER_ID)));

            repository.findAll(PageRequest.of(2, 10));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).query(anyString(), captor.capture(), any(RowMapper.class));
            assertThat(captor.getValue().getValue("limite")).isEqualTo(10);
            assertThat(captor.getValue().getValue("deslocamento")).isEqualTo(20L);
        }

        @Test
        @DisplayName("busca por nome usa LIKE sem diferenciar maiúsculas de minúsculas")
        void porNome_deveUsarLikeSemCaixa() {
            totalDe(1L);
            devolverUsuarios(List.of(usuario(USER_ID)));

            repository.findByNameContainingIgnoreCase("Jo", PageRequest.of(0, 20));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).query(anyString(), captor.capture(), any(RowMapper.class));
            assertThat(captor.getValue().getValue("padrao")).isEqualTo("%Jo%");
            assertThat(sqlDaPagina()).contains("LOWER(name) LIKE LOWER(:padrao)");
        }

        @Test
        @DisplayName("sem ordenação explícita, ordena por nome")
        void semOrdenacao_deveUsarOPadrao() {
            totalDe(1L);
            devolverUsuarios(List.of(usuario(USER_ID)));

            repository.findAll(PageRequest.of(0, 20));

            assertThat(sqlDaPagina()).contains("ORDER BY name ASC");
        }

        @Test
        @DisplayName("ordenação por propriedade conhecida vira a coluna correspondente")
        void ordenacaoConhecida_deveVirarColuna() {
            totalDe(1L);
            devolverUsuarios(List.of(usuario(USER_ID)));

            repository.findAll(PageRequest.of(0, 20,
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("email"))));

            assertThat(sqlDaPagina()).contains("ORDER BY created_at DESC, email ASC");
        }

        /**
         * O nome da coluna entra no ORDER BY por concatenação — não há como
         * parametrizá-lo. Por isso só passam as propriedades da lista permitida:
         * qualquer outra é descartada, e a ordenação cai no padrão.
         */
        @Test
        @DisplayName("propriedade fora da lista permitida é ignorada e cai no padrão")
        void ordenacaoDesconhecida_deveCairNoPadrao() {
            totalDe(1L);
            devolverUsuarios(List.of(usuario(USER_ID)));

            repository.findAll(PageRequest.of(0, 20, Sort.by("password")));

            assertThat(sqlDaPagina()).contains("ORDER BY name ASC").doesNotContain("password");
        }

        @Test
        @DisplayName("Pageable sem ordenação declarada cai no padrão")
        void pageableNaoOrdenado_deveCairNoPadrao() {
            totalDe(1L);
            devolverUsuarios(List.of(usuario(USER_ID)));

            repository.findAll(PageRequest.of(0, 20, Sort.unsorted()));

            assertThat(sqlDaPagina()).contains("ORDER BY name ASC");
        }

        /** Guarda contra implementações de Pageable que devolvem Sort nulo. */
        @Test
        @DisplayName("Pageable que devolve Sort nulo cai no padrão em vez de falhar")
        void pageableComSortNulo_deveCairNoPadrao() {
            totalDe(1L);
            devolverUsuarios(List.of(usuario(USER_ID)));
            Pageable pageable = mock(Pageable.class);
            when(pageable.getPageSize()).thenReturn(20);
            when(pageable.getOffset()).thenReturn(0L);
            when(pageable.getSort()).thenReturn(null);

            repository.findAll(pageable);

            assertThat(sqlDaPagina()).contains("ORDER BY name ASC");
        }
    }

    @Nested
    @DisplayName("gravação do agregado")
    class Gravacao {

        @Test
        @DisplayName("usuário sem id é inserido e recebe o id gerado e as marcas de criação")
        void save_semId_deveInserir() {
            when(auditorProvider.currentAuditor()).thenReturn("system");
            when(jdbc.queryForObject(contains("INSERT INTO users"),
                    any(SqlParameterSource.class), eq(UUID.class))).thenReturn(USER_ID);

            User salvo = repository.save(usuario(null));

            assertThat(salvo.getId()).isEqualTo(USER_ID);
            assertThat(salvo.getCreatedBy()).isEqualTo("system");
            assertThat(salvo.getCreatedAt()).isNotNull().isEqualTo(salvo.getLastUpdatedAt());
            verify(jdbc, never()).update(contains("UPDATE users"), any(SqlParameterSource.class));
        }

        @Test
        @DisplayName("usuário com id é atualizado, preservando quem o criou")
        void save_comId_deveAtualizar() {
            User existente = usuario(USER_ID);
            existente.restoreAudit(CRIACAO, CRIACAO, "system", "system");
            when(auditorProvider.currentAuditor()).thenReturn("joao.silva");

            repository.save(existente);

            assertThat(existente.getCreatedAt()).isEqualTo(CRIACAO);
            assertThat(existente.getCreatedBy()).isEqualTo("system");
            assertThat(existente.getLastUpdatedBy()).isEqualTo("joao.silva");

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(contains("UPDATE users"), captor.capture());
            assertThat(captor.getValue().getValue("id")).isEqualTo(USER_ID);
            assertThat(captor.getValue().getValue("password")).isEqualTo("HASH");
        }

        /** Equivalente ao @ManyToMany gerenciado: os vínculos são reescritos. */
        @Test
        @DisplayName("papéis são reescritos na tabela associativa")
        void save_comPapeis_deveReescreverOsVinculos() {
            User user = usuario(USER_ID);
            user.setRoles(Set.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER)));

            repository.save(user);

            verify(jdbc).update(contains("DELETE FROM user_roles"), any(SqlParameterSource.class));

            ArgumentCaptor<SqlParameterSource[]> captor = ArgumentCaptor.forClass(SqlParameterSource[].class);
            verify(jdbc).batchUpdate(contains("INSERT INTO user_roles"), captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue()[0].getValue("roleId")).isEqualTo(ROLE_ID);
        }

        @Test
        @DisplayName("usuário sem papéis apenas apaga os vínculos existentes")
        void save_semPapeis_naoDeveInserirVinculos() {
            repository.save(usuario(USER_ID));

            verify(jdbc).update(contains("DELETE FROM user_roles"), any(SqlParameterSource.class));
            verify(jdbc, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
        }

        @Test
        @DisplayName("endereço novo é inserido já vinculado ao usuário")
        void save_comEnderecoNovo_deveInserir() {
            User user = usuario(USER_ID);
            user.getAddresses().add(Address.builder().street("Rua Nova").zipCode("60175047").build());
            when(auditorProvider.currentAuditor()).thenReturn("system");
            when(jdbc.queryForObject(contains("INSERT INTO addresses"),
                    any(SqlParameterSource.class), eq(UUID.class))).thenReturn(ADDRESS_ID);

            repository.save(user);

            Address endereco = user.getAddresses().get(0);
            assertThat(endereco.getId()).isEqualTo(ADDRESS_ID);
            assertThat(endereco.getUserId()).isEqualTo(USER_ID);
            assertThat(endereco.getCreatedBy()).isEqualTo("system");
        }

        @Test
        @DisplayName("endereço já existente é atualizado, e os ausentes são removidos")
        void save_comEnderecoExistente_deveAtualizarERemoverAusentes() {
            User user = usuario(USER_ID);
            Address endereco = Address.builder().id(ADDRESS_ID).street("Rua Antiga").build();
            endereco.restoreAudit(CRIACAO, CRIACAO, "system", "system");
            user.getAddresses().add(endereco);
            when(auditorProvider.currentAuditor()).thenReturn("joao.silva");

            repository.save(user);

            assertThat(endereco.getCreatedBy()).isEqualTo("system");
            assertThat(endereco.getLastUpdatedBy()).isEqualTo("joao.silva");

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(contains("DELETE FROM addresses"), captor.capture());
            assertThat(captor.getValue().getValue("ids")).isEqualTo(List.of(ADDRESS_ID));
            verify(jdbc).update(contains("UPDATE addresses"), any(SqlParameterSource.class));
        }

        /** Equivalente ao orphanRemoval: sem endereços na entidade, apaga todos. */
        @Test
        @DisplayName("usuário sem endereços tem todos os endereços removidos")
        void save_semEnderecos_deveRemoverTodos() {
            repository.save(usuario(USER_ID));

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(eq("DELETE FROM addresses WHERE user_id = :userId"), captor.capture());
            assertThat(captor.getValue().getValue("userId")).isEqualTo(USER_ID);
            verify(jdbc, never()).update(contains("NOT IN"), any(SqlParameterSource.class));
        }

        @Test
        @DisplayName("lista de endereços nula é tratada como lista vazia")
        void save_comListaDeEnderecosNula_naoDeveFalhar() {
            User user = usuario(USER_ID);
            user.setAddresses(null);

            repository.save(user);

            verify(jdbc).update(eq("DELETE FROM addresses WHERE user_id = :userId"),
                    any(SqlParameterSource.class));
        }

        @Test
        @DisplayName("exclusão remove o usuário, deixando o cascade do schema cuidar do resto")
        void deleteById_deveRemoverOUsuario() {
            repository.deleteById(USER_ID);

            ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
            verify(jdbc).update(contains("DELETE FROM users"), captor.capture());
            assertThat(captor.getValue().getValue("id")).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("mapeamento do ResultSet")
    class Mapeamento {

        @SuppressWarnings("unchecked")
        private RowMapper<User> capturarRowMapper() {
            ArgumentCaptor<RowMapper<User>> captor = ArgumentCaptor.forClass(RowMapper.class);
            verify(jdbc).query(anyString(), any(SqlParameterSource.class), captor.capture());
            return captor.getValue();
        }

        private List<RowCallbackHandler> capturarCallbacks() {
            ArgumentCaptor<RowCallbackHandler> captor = ArgumentCaptor.forClass(RowCallbackHandler.class);
            verify(jdbc, org.mockito.Mockito.times(2))
                    .query(anyString(), any(SqlParameterSource.class), captor.capture());
            return captor.getAllValues();
        }

        private ResultSet resultSetDeUsuario() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("id", UUID.class)).thenReturn(USER_ID);
            when(rs.getString("name")).thenReturn("João Silva");
            when(rs.getString("email")).thenReturn("joao@email.com");
            when(rs.getString("login")).thenReturn("joao.silva");
            when(rs.getString("password")).thenReturn("HASH");
            when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(CRIACAO);
            when(rs.getObject("last_updated_at", LocalDateTime.class)).thenReturn(CRIACAO);
            when(rs.getString("created_by")).thenReturn("system");
            when(rs.getString("last_updated_by")).thenReturn("admin");
            return rs;
        }

        @Test
        @DisplayName("consulta de um usuário mapeia também o hash da senha")
        void consultaUnica_deveMapearASenha() throws Exception {
            devolverUsuarios(List.of());
            repository.findById(USER_ID);

            User user = capturarRowMapper().mapRow(resultSetDeUsuario(), 1);

            assertThat(user.getId()).isEqualTo(USER_ID);
            assertThat(user.getName()).isEqualTo("João Silva");
            assertThat(user.getEmail()).isEqualTo("joao@email.com");
            assertThat(user.getLogin()).isEqualTo("joao.silva");
            assertThat(user.getPassword()).isEqualTo("HASH");
            assertThat(user.getCreatedAt()).isEqualTo(CRIACAO);
            assertThat(user.getCreatedBy()).isEqualTo("system");
            assertThat(user.getLastUpdatedBy()).isEqualTo("admin");
        }

        /** A listagem não carrega o hash de senha de todos os usuários da página. */
        @Test
        @DisplayName("listagem paginada mapeia o usuário sem o hash da senha")
        void listagem_naoDeveMapearASenha() throws Exception {
            when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                    .thenReturn(1L);
            devolverUsuarios(List.of());
            repository.findAll(PageRequest.of(0, 20));

            User user = capturarRowMapper().mapRow(resultSetDeUsuario(), 1);

            assertThat(user.getId()).isEqualTo(USER_ID);
            assertThat(user.getPassword()).isNull();
        }

        @Test
        @DisplayName("os papéis carregados são acumulados no usuário correspondente")
        void callbackDePapeis_deveAcumularNoUsuario() throws Exception {
            User user = usuario(USER_ID);
            devolverUsuarios(List.of(user));
            repository.findById(USER_ID);

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("owner_id", UUID.class)).thenReturn(USER_ID);
            when(rs.getObject("id", UUID.class)).thenReturn(ROLE_ID);
            when(rs.getString("name")).thenReturn("ROLE_CUSTOMER");
            when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(CRIACAO);
            when(rs.getObject("last_updated_at", LocalDateTime.class)).thenReturn(CRIACAO);
            when(rs.getString("created_by")).thenReturn("system");
            when(rs.getString("last_updated_by")).thenReturn("system");

            capturarCallbacks().get(0).processRow(rs);

            assertThat(user.getRoles()).singleElement().satisfies(papel -> {
                assertThat(papel.getId()).isEqualTo(ROLE_ID);
                assertThat(papel.getName()).isEqualTo(RoleName.ROLE_CUSTOMER);
                assertThat(papel.getCreatedBy()).isEqualTo("system");
            });
        }

        @Test
        @DisplayName("os endereços carregados são acumulados no usuário correspondente")
        void callbackDeEnderecos_deveAcumularNoUsuario() throws Exception {
            User user = usuario(USER_ID);
            devolverUsuarios(List.of(user));
            repository.findById(USER_ID);

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("id", UUID.class)).thenReturn(ADDRESS_ID);
            when(rs.getObject("user_id", UUID.class)).thenReturn(USER_ID);
            when(rs.getString("street")).thenReturn("Rua das Flores");
            when(rs.getString("number")).thenReturn("100");
            when(rs.getString("complement")).thenReturn("Apto 202");
            when(rs.getString("neighborhood")).thenReturn("Centro");
            when(rs.getString("city")).thenReturn("Fortaleza");
            when(rs.getString("state")).thenReturn("CE");
            when(rs.getString("zip_code")).thenReturn("60175047");
            when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(CRIACAO);
            when(rs.getObject("last_updated_at", LocalDateTime.class)).thenReturn(CRIACAO);
            when(rs.getString("created_by")).thenReturn("system");
            when(rs.getString("last_updated_by")).thenReturn("system");

            capturarCallbacks().get(1).processRow(rs);

            assertThat(user.getAddresses()).singleElement().satisfies(endereco -> {
                assertThat(endereco.getId()).isEqualTo(ADDRESS_ID);
                assertThat(endereco.getUserId()).isEqualTo(USER_ID);
                assertThat(endereco.getStreet()).isEqualTo("Rua das Flores");
                assertThat(endereco.getNumber()).isEqualTo("100");
                assertThat(endereco.getComplement()).isEqualTo("Apto 202");
                assertThat(endereco.getNeighborhood()).isEqualTo("Centro");
                assertThat(endereco.getCity()).isEqualTo("Fortaleza");
                assertThat(endereco.getState()).isEqualTo("CE");
                assertThat(endereco.getZipCode()).isEqualTo("60175047");
                assertThat(endereco.getCreatedBy()).isEqualTo("system");
            });
        }
    }
}

package com.postech.restaurantes.adapter.out.persistence;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageResult;
import com.postech.restaurantes.application.port.out.AuditorPort;
import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.DeleteUserPort;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.domain.model.Address;
import com.postech.restaurantes.domain.model.Role;
import com.postech.restaurantes.domain.model.RoleName;
import com.postech.restaurantes.domain.model.User;
import com.postech.restaurantes.domain.model.shared.Email;
import com.postech.restaurantes.domain.model.shared.ZipCode;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter de saída de persistência do usuário — a implementação em JDBC dos ports
 * de dados.
 *
 * <p>Esta classe é o único lugar do sistema que sabe que existe uma tabela
 * {@code users}, que endereços têm uma coluna {@code user_id} e que papéis moram em
 * uma tabela associativa. Tudo isso é vocabulário de banco, e ele para aqui: o que
 * atravessa a fronteira para dentro do hexágono são objetos de domínio.</p>
 *
 * <p>Uma classe implementando quatro ports pode parecer contradizer a Segregação de
 * Interfaces, mas não contradiz: o princípio fala sobre o que os <em>clientes</em>
 * enxergam. Cada caso de uso continua dependendo apenas do port de que precisa; que
 * a mesma peça de infraestrutura atenda a vários é um detalhe do lado de fora — e
 * um que evita espalhar o mapeamento do agregado por quatro arquivos.</p>
 */
@Component
public class UserPersistenceAdapter
        implements LoadUserPort, SaveUserPort, DeleteUserPort, CheckUserExistsPort {

    private static final String COLUNAS_BASE =
            "id, name, email, login, created_at, last_updated_at";

    /**
     * Colunas das buscas que devolvem um único usuário.
     *
     * <p>Precisam do hash da senha: findByLogin alimenta a autenticação, e o
     * usuário lido por findById volta para o save — que reescreve a coluna
     * password. Carregar sem o hash e salvar em seguida apagaria a senha, então
     * leitura completa e gravação completa andam juntas.</p>
     */
    private static final String COLUNAS = COLUNAS_BASE + ", password";

    /**
     * Colunas da listagem paginada — sem o hash da senha.
     *
     * <p>Nenhum consumidor precisa dele (a UserView não o expõe e a página nunca é
     * gravada de volta), e trazê-lo colocaria o hash de todos os usuários da página
     * em memória sem motivo.</p>
     */
    private static final String COLUNAS_LISTAGEM = COLUNAS_BASE;

    /**
     * Propriedades aceitas em ?sort=... mapeadas para colunas reais.
     *
     * <p>O nome da coluna entra no ORDER BY por concatenação — não há como
     * parametrizá-lo — então ele nunca pode vir direto da requisição. Só passam as
     * chaves deste mapa; qualquer outra propriedade é ignorada. "password" está
     * fora de propósito.</p>
     */
    private static final Map<String, String> COLUNAS_ORDENAVEIS = Map.of(
            "id", "id",
            "name", "name",
            "email", "email",
            "login", "login",
            "createdAt", "created_at",
            "lastUpdatedAt", "last_updated_at");

    private static final String ORDENACAO_PADRAO = "name ASC";

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditorPort auditorPort;

    public UserPersistenceAdapter(NamedParameterJdbcTemplate jdbc, AuditorPort auditorPort) {
        this.jdbc = jdbc;
        this.auditorPort = auditorPort;
    }

    // ----- LoadUserPort -----

    @Override
    public Optional<User> findById(UUID id) {
        return buscarUm("id = :valor", id);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return buscarUm("login = :valor", login);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return buscarUm("email = :valor", email);
    }

    @Override
    public PageResult<User> findAll(PageQuery pageQuery) {
        return buscarPagina("", new MapSqlParameterSource(), pageQuery);
    }

    @Override
    public PageResult<User> findByNameContaining(String name, PageQuery pageQuery) {
        return buscarPagina(" WHERE LOWER(name) LIKE LOWER(:padrao)",
                new MapSqlParameterSource("padrao", "%" + name + "%"), pageQuery);
    }

    // ----- CheckUserExistsPort -----

    @Override
    public boolean existsById(UUID id) {
        return consultarExistencia(
                "SELECT EXISTS(SELECT 1 FROM users WHERE id = :id)",
                new MapSqlParameterSource("id", id));
    }

    @Override
    public boolean existsByEmailExcluding(String email, UUID currentUserId) {
        return existsExcluding("email", email, currentUserId);
    }

    @Override
    public boolean existsByLoginExcluding(String login, UUID currentUserId) {
        return existsExcluding("login", login, currentUserId);
    }

    /**
     * O id atual entra na cláusula para ser ignorado. Como {@code <> NULL} nunca é
     * verdadeiro em SQL, o {@code IS NULL} cobre o caso do cadastro, em que não há
     * registro a excluir da checagem.
     *
     * <p>O {@code CAST(... AS uuid)} é obrigatório: em {@code ? IS NULL} o
     * PostgreSQL não tem de onde inferir o tipo do parâmetro e recusa a instrução
     * com erro de sintaxe. O cast explícito resolve a ambiguidade.</p>
     */
    private boolean existsExcluding(String coluna, String valor, UUID currentUserId) {
        return consultarExistencia(
                "SELECT EXISTS(SELECT 1 FROM users WHERE " + coluna + " = :valor"
                        + " AND (CAST(:atual AS uuid) IS NULL OR id <> CAST(:atual AS uuid)))",
                new MapSqlParameterSource()
                        .addValue("valor", valor)
                        .addValue("atual", currentUserId));
    }

    private boolean consultarExistencia(String sql, SqlParameterSource parametros) {
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, parametros, Boolean.class));
    }

    // ----- SaveUserPort / DeleteUserPort -----

    /**
     * Grava o agregado inteiro: o usuário, seus papéis e seus endereços.
     *
     * <p>É o que o ORM da variante original fazia por cascade e orphanRemoval, aqui
     * explícito em SQL. A atomicidade do conjunto é garantida pelo TransactionPort,
     * cuja fronteira é aberta pelo caso de uso.</p>
     */
    @Override
    public User save(User user) {
        String auditor = auditorPort.currentAuditor();
        LocalDateTime agora = LocalDateTime.now();

        // As datas do usuário vêm do domínio (newUser/touch), não do relógio do
        // adapter: "data da última alteração" é regra de negócio da fase. Já o
        // auditor e as datas dos endereços são metadados da gravação, resolvidos aqui.
        if (user.getId() == null) {
            user.assignId(inserir(user, auditor));
        } else {
            atualizar(user, auditor);
        }

        sincronizarPapeis(user);
        sincronizarEnderecos(user, auditor, agora);
        return user;
    }

    @Override
    public void deleteById(UUID id) {
        // user_roles e addresses saem junto pelo ON DELETE CASCADE do schema.
        jdbc.update("DELETE FROM users WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    // ----- leitura -----

    private Optional<User> buscarUm(String condicao, Object valor) {
        List<UserRow> linhas = jdbc.query(
                "SELECT " + COLUNAS + " FROM users WHERE " + condicao,
                new MapSqlParameterSource("valor", valor),
                (rs, i) -> mapearLinha(rs, true));
        return montar(linhas).stream().findFirst();
    }

    private PageResult<User> buscarPagina(String filtro, MapSqlParameterSource parametros,
                                          PageQuery pageQuery) {
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM users" + filtro, parametros, Long.class);
        if (total == null || total == 0) {
            return PageResult.empty(pageQuery);
        }

        MapSqlParameterSource paginacao = new MapSqlParameterSource(parametros.getValues())
                .addValue("limite", pageQuery.size())
                .addValue("deslocamento", pageQuery.offset());

        List<UserRow> linhas = jdbc.query(
                "SELECT " + COLUNAS_LISTAGEM + " FROM users" + filtro
                        + " ORDER BY " + ordenacao(pageQuery)
                        + " LIMIT :limite OFFSET :deslocamento",
                paginacao,
                (rs, i) -> mapearLinha(rs, false));

        return PageResult.of(montar(linhas), pageQuery, total);
    }

    private String ordenacao(PageQuery pageQuery) {
        // A consulta pode vir sem ordenação declarada (PageQuery.of, ou um Pageable
        // sem sort): o mapa é imutável e recusa chave nula, então a guarda vem antes
        // da busca.
        if (pageQuery.sortBy() == null) {
            return ORDENACAO_PADRAO;
        }

        String coluna = COLUNAS_ORDENAVEIS.get(pageQuery.sortBy());
        if (coluna == null) {
            return ORDENACAO_PADRAO;
        }
        return coluna + (pageQuery.direction() == PageQuery.SortDirection.DESC ? " DESC" : " ASC");
    }

    /**
     * Carrega papéis e endereços de todos os usuários da página em duas consultas
     * (uma por associação), e não uma por usuário — o N+1 que o fetch do ORM
     * escondia fica visível e resolvido aqui.
     */
    private List<User> montar(List<UserRow> linhas) {
        if (linhas.isEmpty()) {
            return List.of();
        }

        Map<UUID, UserRow> porId = linhas.stream()
                .collect(Collectors.toMap(UserRow::id, linha -> linha, (a, b) -> a, LinkedHashMap::new));
        SqlParameterSource parametros = new MapSqlParameterSource("userIds", porId.keySet());

        RowCallbackHandler acumularPapeis = rs ->
                porId.get(rs.getObject("owner_id", UUID.class)).roles().add(mapearPapel(rs));
        jdbc.query("""
                SELECT ur.user_id AS owner_id, r.id, r.name
                  FROM user_roles ur
                  JOIN roles r ON r.id = ur.role_id
                 WHERE ur.user_id IN (:userIds)
                """, parametros, acumularPapeis);

        RowCallbackHandler acumularEnderecos = rs ->
                porId.get(rs.getObject("user_id", UUID.class)).addresses().add(mapearEndereco(rs));
        jdbc.query("""
                SELECT id, user_id, street, number, complement, neighborhood, city, state, zip_code
                  FROM addresses
                 WHERE user_id IN (:userIds)
                 ORDER BY created_at, id
                """, parametros, acumularEnderecos);

        return porId.values().stream().map(UserRow::toDomain).toList();
    }

    private UserRow mapearLinha(ResultSet rs, boolean comSenha) throws SQLException {
        return new UserRow(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("login"),
                comSenha ? rs.getString("password") : null,
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("last_updated_at", LocalDateTime.class),
                new HashSet<>(),
                new ArrayList<>());
    }

    private Role mapearPapel(ResultSet rs) throws SQLException {
        return Role.restore(
                rs.getObject("id", UUID.class),
                RoleName.valueOf(rs.getString("name")));
    }

    private Address mapearEndereco(ResultSet rs) throws SQLException {
        return Address.restore(
                rs.getObject("id", UUID.class),
                rs.getString("street"),
                rs.getString("number"),
                rs.getString("complement"),
                rs.getString("neighborhood"),
                rs.getString("city"),
                rs.getString("state"),
                new ZipCode(rs.getString("zip_code")));
    }

    /**
     * Linha crua de {@code users}, ainda sem as associações.
     *
     * <p>Existe porque a entidade de domínio é construída de uma vez, com papéis e
     * endereços já resolvidos — não há como criar um {@code User} vazio e ir
     * preenchendo, que é justamente a rigidez que se quer do domínio. Esta é a
     * estrutura mutável de trabalho que absorve essa diferença, e ela não sai do
     * adapter.</p>
     */
    private record UserRow(UUID id, String name, String email, String login, String password,
                           LocalDateTime createdAt, LocalDateTime lastUpdatedAt,
                           Set<Role> roles, List<Address> addresses) {

        User toDomain() {
            return User.restore(id, name, new Email(email), login, password,
                    roles, addresses, createdAt, lastUpdatedAt);
        }
    }

    // ----- escrita -----

    private UUID inserir(User usuario, String auditor) {
        return jdbc.queryForObject("""
                INSERT INTO users (name, email, login, password,
                                   created_at, last_updated_at, created_by, last_updated_by)
                VALUES (:name, :email, :login, :password,
                        :createdAt, :lastUpdatedAt, :auditor, :auditor)
                RETURNING id
                """, parametrosDe(usuario)
                .addValue("createdAt", usuario.getCreatedAt())
                .addValue("lastUpdatedAt", usuario.getLastUpdatedAt())
                .addValue("auditor", auditor), UUID.class);
    }

    private void atualizar(User usuario, String auditor) {
        // created_at / created_by não entram: quem criou o registro não muda.
        jdbc.update("""
                UPDATE users
                   SET name = :name, email = :email, login = :login, password = :password,
                       last_updated_at = :lastUpdatedAt, last_updated_by = :auditor
                 WHERE id = :id
                """, parametrosDe(usuario)
                .addValue("lastUpdatedAt", usuario.getLastUpdatedAt())
                .addValue("auditor", auditor)
                .addValue("id", usuario.getId()));
    }

    private MapSqlParameterSource parametrosDe(User usuario) {
        return new MapSqlParameterSource()
                .addValue("name", usuario.getName())
                .addValue("email", usuario.getEmail().value())
                .addValue("login", usuario.getLogin())
                .addValue("password", usuario.getPassword());
    }

    /** Reescreve os vínculos de papel do usuário (equivalente ao @ManyToMany gerenciado). */
    private void sincronizarPapeis(User usuario) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = :userId",
                new MapSqlParameterSource("userId", usuario.getId()));

        if (usuario.getRoles().isEmpty()) {
            return;
        }

        SqlParameterSource[] vinculos = usuario.getRoles().stream()
                .map(papel -> new MapSqlParameterSource()
                        .addValue("userId", usuario.getId())
                        .addValue("roleId", papel.getId()))
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate("INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)", vinculos);
    }

    /**
     * Equivalente ao cascade + orphanRemoval: remove os endereços que saíram da
     * lista e grava os que ficaram.
     */
    private void sincronizarEnderecos(User usuario, String auditor, LocalDateTime agora) {
        List<Address> enderecos = usuario.getAddresses();
        removerEnderecosAusentes(usuario.getId(), enderecos);

        for (Address endereco : enderecos) {
            if (endereco.getId() == null) {
                endereco.assignId(inserirEndereco(usuario.getId(), endereco, auditor, agora));
            } else {
                atualizarEndereco(usuario.getId(), endereco, auditor, agora);
            }
        }
    }

    private void removerEnderecosAusentes(UUID userId, List<Address> mantidos) {
        List<UUID> idsMantidos = mantidos.stream()
                .map(Address::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (idsMantidos.isEmpty()) {
            jdbc.update("DELETE FROM addresses WHERE user_id = :userId",
                    new MapSqlParameterSource("userId", userId));
            return;
        }

        jdbc.update("DELETE FROM addresses WHERE user_id = :userId AND id NOT IN (:ids)",
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("ids", idsMantidos));
    }

    private UUID inserirEndereco(UUID userId, Address endereco, String auditor, LocalDateTime agora) {
        return jdbc.queryForObject("""
                INSERT INTO addresses (user_id, street, number, complement, neighborhood,
                                       city, state, zip_code,
                                       created_at, last_updated_at, created_by, last_updated_by)
                VALUES (:userId, :street, :number, :complement, :neighborhood,
                        :city, :state, :zipCode,
                        :agora, :agora, :auditor, :auditor)
                RETURNING id
                """, parametrosDe(userId, endereco, auditor, agora), UUID.class);
    }

    private void atualizarEndereco(UUID userId, Address endereco, String auditor, LocalDateTime agora) {
        jdbc.update("""
                UPDATE addresses
                   SET user_id = :userId, street = :street, number = :number,
                       complement = :complement, neighborhood = :neighborhood,
                       city = :city, state = :state, zip_code = :zipCode,
                       last_updated_at = :agora, last_updated_by = :auditor
                 WHERE id = :id
                """, parametrosDe(userId, endereco, auditor, agora).addValue("id", endereco.getId()));
    }

    private MapSqlParameterSource parametrosDe(UUID userId, Address endereco,
                                               String auditor, LocalDateTime agora) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("street", endereco.getStreet())
                .addValue("number", endereco.getNumber())
                .addValue("complement", endereco.getComplement())
                .addValue("neighborhood", endereco.getNeighborhood())
                .addValue("city", endereco.getCity())
                .addValue("state", endereco.getState())
                .addValue("zipCode", endereco.getZipCode().value())
                .addValue("auditor", auditor)
                .addValue("agora", agora);
    }
}

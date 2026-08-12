package com.postech.restaurantes.repository;

import com.postech.restaurantes.config.AuditorProvider;
import com.postech.restaurantes.entity.Address;
import com.postech.restaurantes.entity.Auditable;
import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.enums.RoleName;
import com.postech.restaurantes.util.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação em JDBC do repositório de usuário.
 *
 * O que o ORM fazia implicitamente passa a ser explícito aqui: o cascade e o
 * orphanRemoval viram INSERT/UPDATE/DELETE dos endereços, o @ManyToMany vira a
 * sincronização da tabela user_roles, e a auditoria — antes preenchida pelo
 * AuditingEntityListener — é aplicada antes de cada gravação.
 */
@Repository
public class UserRepositoryJdbc implements UserRepository {

    private static final String COLUNAS_BASE =
            "id, name, email, login, created_at, last_updated_at, created_by, last_updated_by";

    /**
     * Colunas das buscas que devolvem um único usuário.
     *
     * Estas precisam do hash da senha: findByLogin alimenta a autenticação, e o
     * resultado de findById é devolvido ao save — que reescreve a coluna
     * password. Carregar o usuário sem o hash e salvá-lo em seguida apagaria a
     * senha, então este par (leitura completa + gravação completa) anda junto.
     */
    private static final String COLUNAS = COLUNAS_BASE + ", password";

    /**
     * Colunas da listagem paginada.
     *
     * Sem o hash da senha: nenhum consumidor precisa dele (o UserResponse não o
     * expõe e a página nunca é gravada de volta), e trazê-lo colocaria o hash de
     * todos os usuários da página em memória sem motivo.
     */
    private static final String COLUNAS_LISTAGEM = COLUNAS_BASE;

    /**
     * Propriedades aceitas em ?sort=... mapeadas para colunas reais.
     *
     * O nome da coluna entra na cláusula ORDER BY por concatenação — não há como
     * parametrizá-lo — então ele nunca pode vir direto da requisição. Só passam
     * as chaves deste mapa; qualquer outra propriedade é ignorada. "password"
     * está fora de propósito.
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
    private final AuditorProvider auditorProvider;

    public UserRepositoryJdbc(NamedParameterJdbcTemplate jdbc, AuditorProvider auditorProvider) {
        this.jdbc = jdbc;
        this.auditorProvider = auditorProvider;
    }

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
    public boolean existsById(UUID id) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM users WHERE id = :id)",
                new MapSqlParameterSource("id", id), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return buscarPagina("", new MapSqlParameterSource(), pageable);
    }

    @Override
    public Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable) {
        return buscarPagina(" WHERE LOWER(name) LIKE LOWER(:padrao)",
                new MapSqlParameterSource("padrao", "%" + name + "%"), pageable);
    }

    @Override
    public User save(User user) {
        String auditor = auditorProvider.currentAuditor();
        LocalDateTime agora = LocalDateTime.now();

        if (user.getId() == null) {
            user.markCreated(auditor, agora);
            user.setId(inserir(user));
        } else {
            user.markUpdated(auditor, agora);
            atualizar(user);
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
        List<User> encontrados = jdbc.query(
                "SELECT " + COLUNAS + " FROM users WHERE " + condicao,
                new MapSqlParameterSource("valor", valor), this::mapearUsuario);
        return carregarAssociacoes(encontrados).stream().findFirst();
    }

    private Page<User> buscarPagina(String filtro, MapSqlParameterSource parametros, Pageable pageable) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM users" + filtro, parametros, Long.class);
        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        MapSqlParameterSource paginacao = new MapSqlParameterSource(parametros.getValues())
                .addValue("limite", pageable.getPageSize())
                .addValue("deslocamento", pageable.getOffset());

        List<User> usuarios = jdbc.query(
                "SELECT " + COLUNAS_LISTAGEM + " FROM users" + filtro
                        + " ORDER BY " + ordenacao(pageable.getSort())
                        + " LIMIT :limite OFFSET :deslocamento",
                paginacao, this::mapearUsuarioSemSenha);

        return new PageImpl<>(carregarAssociacoes(usuarios), pageable, total);
    }

    private String ordenacao(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return ORDENACAO_PADRAO;
        }
        String clausulas = sort.stream()
                .filter(ordem -> COLUNAS_ORDENAVEIS.containsKey(ordem.getProperty()))
                .map(ordem -> COLUNAS_ORDENAVEIS.get(ordem.getProperty())
                        + (ordem.isDescending() ? " DESC" : " ASC"))
                .collect(Collectors.joining(", "));
        return clausulas.isBlank() ? ORDENACAO_PADRAO : clausulas;
    }

    /**
     * Carrega papéis e endereços de todos os usuários da página em duas consultas
     * (uma por associação), e não uma por usuário — o N+1 que o fetch do ORM
     * escondia fica visível e resolvido aqui.
     */
    private List<User> carregarAssociacoes(List<User> usuarios) {
        if (usuarios.isEmpty()) {
            return usuarios;
        }

        Map<UUID, User> porId = usuarios.stream()
                .collect(Collectors.toMap(User::getId, usuario -> usuario, (a, b) -> a, LinkedHashMap::new));
        SqlParameterSource parametros = new MapSqlParameterSource("userIds", porId.keySet());

        RowCallbackHandler acumularPapeis = rs ->
                porId.get(rs.getObject("owner_id", UUID.class)).getRoles().add(mapearPapel(rs));
        jdbc.query("""
                SELECT ur.user_id AS owner_id,
                       r.id, r.name, r.created_at, r.last_updated_at, r.created_by, r.last_updated_by
                  FROM user_roles ur
                  JOIN roles r ON r.id = ur.role_id
                 WHERE ur.user_id IN (:userIds)
                """, parametros, acumularPapeis);

        RowCallbackHandler acumularEnderecos = rs -> {
            Address endereco = mapearEndereco(rs);
            porId.get(endereco.getUserId()).getAddresses().add(endereco);
        };
        jdbc.query("""
                SELECT id, user_id, street, number, complement, neighborhood, city, state, zip_code,
                       created_at, last_updated_at, created_by, last_updated_by
                  FROM addresses
                 WHERE user_id IN (:userIds)
                 ORDER BY created_at, id
                """, parametros, acumularEnderecos);

        return usuarios;
    }

    /** Mapeia o usuário incluindo o hash da senha (consultas com COLUNAS). */
    private User mapearUsuario(ResultSet rs, int linha) throws SQLException {
        User usuario = mapearUsuarioSemSenha(rs, linha);
        usuario.setPassword(rs.getString("password"));
        return usuario;
    }

    /** Mapeia o usuário sem o hash da senha (consultas com COLUNAS_LISTAGEM). */
    private User mapearUsuarioSemSenha(ResultSet rs, int linha) throws SQLException {
        User usuario = new User();
        usuario.setId(rs.getObject("id", UUID.class));
        usuario.setName(rs.getString("name"));
        usuario.setEmail(rs.getString("email"));
        usuario.setLogin(rs.getString("login"));
        restaurarAuditoria(usuario, rs);
        return usuario;
    }

    private Role mapearPapel(ResultSet rs) throws SQLException {
        Role papel = new Role();
        papel.setId(rs.getObject("id", UUID.class));
        papel.setName(RoleName.valueOf(rs.getString("name")));
        restaurarAuditoria(papel, rs);
        return papel;
    }

    private Address mapearEndereco(ResultSet rs) throws SQLException {
        Address endereco = new Address();
        endereco.setId(rs.getObject("id", UUID.class));
        endereco.setUserId(rs.getObject("user_id", UUID.class));
        endereco.setStreet(rs.getString("street"));
        endereco.setNumber(rs.getString("number"));
        endereco.setComplement(rs.getString("complement"));
        endereco.setNeighborhood(rs.getString("neighborhood"));
        endereco.setCity(rs.getString("city"));
        endereco.setState(rs.getString("state"));
        endereco.setZipCode(rs.getString("zip_code"));
        restaurarAuditoria(endereco, rs);
        return endereco;
    }

    private void restaurarAuditoria(Auditable entidade, ResultSet rs) throws SQLException {
        entidade.restoreAudit(
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("last_updated_at", LocalDateTime.class),
                rs.getString("created_by"),
                rs.getString("last_updated_by"));
    }

    // ----- escrita -----

    private UUID inserir(User usuario) {
        return jdbc.queryForObject("""
                INSERT INTO users (name, email, login, password,
                                   created_at, last_updated_at, created_by, last_updated_by)
                VALUES (:name, :email, :login, :password,
                        :createdAt, :lastUpdatedAt, :createdBy, :lastUpdatedBy)
                RETURNING id
                """, parametrosDe(usuario), UUID.class);
    }

    private void atualizar(User usuario) {
        // created_at / created_by não entram: quem criou o registro não muda.
        jdbc.update("""
                UPDATE users
                   SET name = :name, email = :email, login = :login, password = :password,
                       last_updated_at = :lastUpdatedAt, last_updated_by = :lastUpdatedBy
                 WHERE id = :id
                """, parametrosDe(usuario).addValue("id", usuario.getId()));
    }

    private MapSqlParameterSource parametrosDe(User usuario) {
        return new MapSqlParameterSource()
                .addValue("name", usuario.getName())
                .addValue("email", usuario.getEmail())
                .addValue("login", usuario.getLogin())
                .addValue("password", usuario.getPassword())
                .addValue("createdAt", usuario.getCreatedAt())
                .addValue("lastUpdatedAt", usuario.getLastUpdatedAt())
                .addValue("createdBy", usuario.getCreatedBy())
                .addValue("lastUpdatedBy", usuario.getLastUpdatedBy());
    }

    /** Reescreve os vínculos de papel do usuário (equivalente ao @ManyToMany gerenciado). */
    private void sincronizarPapeis(User usuario) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = :userId",
                new MapSqlParameterSource("userId", usuario.getId()));

        if (ObjectUtils.isEmpty(usuario.getRoles())) {
            return;
        }

        SqlParameterSource[] vinculos = usuario.getRoles().stream()
                .map(papel -> new MapSqlParameterSource()
                        .addValue("userId", usuario.getId())
                        .addValue("roleId", papel.getId()))
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate("INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)", vinculos);
    }

    /** Equivalente ao cascade + orphanRemoval: remove os ausentes e grava os presentes. */
    private void sincronizarEnderecos(User usuario, String auditor, LocalDateTime agora) {
        List<Address> enderecos = usuario.getAddresses() == null ? List.of() : usuario.getAddresses();
        removerEnderecosAusentes(usuario.getId(), enderecos);

        for (Address endereco : enderecos) {
            endereco.setUserId(usuario.getId());
            if (endereco.getId() == null) {
                endereco.markCreated(auditor, agora);
                endereco.setId(inserirEndereco(endereco));
            } else {
                endereco.markUpdated(auditor, agora);
                atualizarEndereco(endereco);
            }
        }
    }

    private void removerEnderecosAusentes(UUID userId, List<Address> mantidos) {
        List<UUID> idsMantidos = mantidos.stream()
                .map(Address::getId)
                .filter(Objects::nonNull)
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

    private UUID inserirEndereco(Address endereco) {
        return jdbc.queryForObject("""
                INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code,
                                       created_at, last_updated_at, created_by, last_updated_by)
                VALUES (:userId, :street, :number, :complement, :neighborhood, :city, :state, :zipCode,
                        :createdAt, :lastUpdatedAt, :createdBy, :lastUpdatedBy)
                RETURNING id
                """, parametrosDe(endereco), UUID.class);
    }

    private void atualizarEndereco(Address endereco) {
        jdbc.update("""
                UPDATE addresses
                   SET user_id = :userId, street = :street, number = :number, complement = :complement,
                       neighborhood = :neighborhood, city = :city, state = :state, zip_code = :zipCode,
                       last_updated_at = :lastUpdatedAt, last_updated_by = :lastUpdatedBy
                 WHERE id = :id
                """, parametrosDe(endereco).addValue("id", endereco.getId()));
    }

    private MapSqlParameterSource parametrosDe(Address endereco) {
        return new MapSqlParameterSource()
                .addValue("userId", endereco.getUserId())
                .addValue("street", endereco.getStreet())
                .addValue("number", endereco.getNumber())
                .addValue("complement", endereco.getComplement())
                .addValue("neighborhood", endereco.getNeighborhood())
                .addValue("city", endereco.getCity())
                .addValue("state", endereco.getState())
                .addValue("zipCode", endereco.getZipCode())
                .addValue("createdAt", endereco.getCreatedAt())
                .addValue("lastUpdatedAt", endereco.getLastUpdatedAt())
                .addValue("createdBy", endereco.getCreatedBy())
                .addValue("lastUpdatedBy", endereco.getLastUpdatedBy());
    }
}

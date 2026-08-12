package com.postech.restaurantes.repository;

import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.enums.RoleName;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em JDBC do repositório de papéis. O nome do papel é gravado
 * como texto na coluna (era o @Enumerated(EnumType.STRING) do JPA), então a
 * conversão de e para o enum é feita explicitamente no mapeamento.
 */
@Repository
public class RoleRepositoryJdbc implements RoleRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public RoleRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Role> findByName(RoleName name) {
        return jdbc.query("""
                SELECT id, name, created_at, last_updated_at, created_by, last_updated_by
                  FROM roles
                 WHERE name = :name
                """, new MapSqlParameterSource("name", name.name()), this::mapearPapel)
                .stream()
                .findFirst();
    }

    private Role mapearPapel(ResultSet rs, int linha) throws SQLException {
        Role papel = new Role();
        papel.setId(rs.getObject("id", UUID.class));
        papel.setName(RoleName.valueOf(rs.getString("name")));
        papel.restoreAudit(
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("last_updated_at", LocalDateTime.class),
                rs.getString("created_by"),
                rs.getString("last_updated_by"));
        return papel;
    }
}

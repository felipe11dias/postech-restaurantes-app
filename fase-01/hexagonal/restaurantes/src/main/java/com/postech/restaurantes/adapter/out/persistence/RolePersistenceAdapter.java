package com.postech.restaurantes.adapter.out.persistence;

import com.postech.restaurantes.application.port.out.LoadRolePort;
import com.postech.restaurantes.domain.model.Role;
import com.postech.restaurantes.domain.model.RoleName;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída para os papéis de autorização. Os registros são criados pelo
 * seed da migration V1, então aqui só há leitura.
 *
 * <p>O nome do papel é gravado como texto na coluna, então a conversão de e para
 * o enum de domínio acontece explicitamente no mapeamento — é o equivalente manual
 * do {@code @Enumerated(EnumType.STRING)} do JPA.</p>
 */
@Component
public class RolePersistenceAdapter implements LoadRolePort {

    private final NamedParameterJdbcTemplate jdbc;

    public RolePersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Role> findByName(RoleName name) {
        return jdbc.query("SELECT id, name FROM roles WHERE name = :name",
                        new MapSqlParameterSource("name", name.name()), this::mapearPapel)
                .stream()
                .findFirst();
    }

    private Role mapearPapel(ResultSet rs, int linha) throws SQLException {
        return Role.restore(
                rs.getObject("id", UUID.class),
                RoleName.valueOf(rs.getString("name")));
    }
}

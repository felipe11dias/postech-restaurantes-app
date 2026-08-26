package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.util.ObjectUtils;

import java.util.Objects;
import java.util.UUID;

/**
 * Papel de autorização. Os registros (ROLE_OWNER, ROLE_CUSTOMER, ROLE_ADMIN) são
 * criados pelo seed da migration e apenas referenciados pelos usuários — por isso
 * o domínio só oferece a reconstrução, não a criação de papéis novos.
 *
 * <p>Os dois tipos de usuário exigidos pela fase (dono de restaurante e cliente)
 * são modelados como papéis, e não como subclasses de {@link User}: um mesmo
 * usuário pode acumular papéis, e a autorização do adapter de segurança lê essa
 * lista diretamente.</p>
 */
public class Role {

    private final UUID id;
    private final RoleName name;

    private Role(UUID id, RoleName name) {
        this.id = id;
        this.name = ObjectUtils.requireNonNull(name, "O nome do papel é obrigatório");
    }

    /** Reconstrói um papel lido do banco. */
    public static Role restore(UUID id, RoleName name) {
        return new Role(id, name);
    }

    public UUID getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }

    /**
     * Igualdade pelo nome do papel, não pelo id: ROLE_ADMIN é ROLE_ADMIN
     * independentemente da linha que o representa. É o que garante que um
     * {@code Set<Role>} não aceite o mesmo papel duas vezes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Role other && name == other.name;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name.name();
    }
}

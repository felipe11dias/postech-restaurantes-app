package com.postech.restaurantes.entity;

import com.postech.restaurantes.enums.RoleName;

import java.util.UUID;

/**
 * Papel de autorização. Os registros (ROLE_OWNER, ROLE_CUSTOMER, ROLE_ADMIN)
 * são carregados na inicialização (seed) e referenciados pelos usuários
 * através da tabela associativa user_roles.
 */
public class Role extends Auditable {

    private UUID id;
    private RoleName name;

    public Role() {
    }

    public Role(UUID id, RoleName name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RoleName getName() {
        return name;
    }

    public void setName(RoleName name) {
        this.name = name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private UUID id;
        private RoleName name;

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(RoleName name) {
            this.name = name;
            return this;
        }

        public Role build() {
            return new Role(id, name);
        }
    }
}

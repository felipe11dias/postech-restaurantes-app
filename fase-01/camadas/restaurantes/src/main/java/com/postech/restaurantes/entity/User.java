package com.postech.restaurantes.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Usuário do sistema. Raiz do agregado: papéis e endereços só são persistidos
 * através dele (ver UserRepositoryJdbc), que reproduz em SQL o que antes era
 * cascade/orphanRemoval do JPA.
 */
public class User extends Auditable {

    private UUID id;
    private String name;
    private String email;
    private String login;
    private String password;

    /** Papéis do usuário, materializados na tabela associativa user_roles. */
    private Set<Role> roles = new HashSet<>();

    /** Endereços do usuário (1:N na tabela addresses). */
    private List<Address> addresses = new ArrayList<>();

    public User() {
    }

    public User(UUID id, String name, String email, String login, String password,
                Set<Role> roles, List<Address> addresses) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.login = login;
        this.password = password;
        this.roles = roles;
        this.addresses = addresses;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    /**
     * Adiciona um endereço mantendo a consistência dos dois lados
     * do relacionamento (boa prática em relações bidirecionais).
     */
    public void addAddress(Address address) {
        addresses.add(address);
        address.setUserId(id);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setUserId(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private UUID id;
        private String name;
        private String email;
        private String login;
        private String password;
        private Set<Role> roles = new HashSet<>();
        private List<Address> addresses = new ArrayList<>();

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder login(String login) {
            this.login = login;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public Builder addresses(List<Address> addresses) {
            this.addresses = addresses;
            return this;
        }

        public User build() {
            return new User(id, name, email, login, password, roles, addresses);
        }
    }
}

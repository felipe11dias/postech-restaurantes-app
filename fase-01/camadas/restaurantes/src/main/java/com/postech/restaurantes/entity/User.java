package com.postech.restaurantes.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends Auditable {

    private UUID id;
    private String name;
    private String email;
    private String login;
    private String password;

    /** Papéis do usuário, materializados na tabela associativa user_roles. */
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /** Endereços do usuário (1:N na tabela addresses). */
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

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
}

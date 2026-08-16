package com.postech.restaurantes.domain;

import com.postech.restaurantes.domain.model.Address;
import com.postech.restaurantes.domain.model.Role;
import com.postech.restaurantes.domain.model.RoleName;
import com.postech.restaurantes.domain.model.User;
import com.postech.restaurantes.domain.model.shared.Email;
import com.postech.restaurantes.domain.model.shared.ZipCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Objetos de domínio prontos para os testes, para não repetir montagem em cada caso. */
public final class DomainFixtures {

    public static final String SENHA_HASH = "$2a$10$hashfalsoparatestes";

    private DomainFixtures() {
    }

    public static Role roleCustomer() {
        return Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER);
    }

    public static Role roleOwner() {
        return Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER);
    }

    public static Address endereco() {
        return Address.newAddress("Av. Paulista", "1500", "Apto 42", "Bela Vista",
                "São Paulo", "SP", new ZipCode("01310-200"));
    }

    public static User novoUsuario() {
        return User.newUser("Maria Silva", new Email("maria@email.com"), "maria.silva",
                SENHA_HASH, Set.of(roleCustomer()), List.of(endereco()));
    }

    /** Usuário já persistido, com id — o que o LoadUserPort devolveria. */
    public static User usuarioPersistido(UUID id) {
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);
        return User.restore(id, "Maria Silva", new Email("maria@email.com"), "maria.silva",
                SENHA_HASH, Set.of(roleCustomer()), List.of(endereco()), ontem, ontem);
    }

    public static User usuarioPersistido() {
        return usuarioPersistido(UUID.randomUUID());
    }
}

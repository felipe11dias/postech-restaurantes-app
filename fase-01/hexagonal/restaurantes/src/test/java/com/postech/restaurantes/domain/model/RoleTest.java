package com.postech.restaurantes.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A igualdade do papel é pelo nome, não pelo id: ROLE_ADMIN é ROLE_ADMIN
 * independentemente da linha que o representa. É isso que impede o mesmo papel
 * de entrar duas vezes no {@code Set<Role>} de um usuário — o que aconteceria se
 * dois registros distintos do mesmo papel chegassem juntos.
 */
class RoleTest {

    @Test
    @DisplayName("restore reconstrói o papel com id e nome")
    void restoreTrazIdENome() {
        UUID id = UUID.randomUUID();

        Role role = Role.restore(id, RoleName.ROLE_OWNER);

        assertEquals(id, role.getId());
        assertEquals(RoleName.ROLE_OWNER, role.getName());
    }

    @Test
    @DisplayName("exige o nome do papel")
    void exigeNome() {
        assertThrows(IllegalArgumentException.class, () -> Role.restore(UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("papéis de mesmo nome são iguais, mesmo com ids diferentes")
    void mesmoNomeIdsDiferentes() {
        Role um = Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER);
        Role outro = Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER);

        assertEquals(um, outro);
        assertEquals(um.hashCode(), outro.hashCode());
    }

    @Test
    @DisplayName("papéis de nomes diferentes não são iguais")
    void nomesDiferentes() {
        assertNotEquals(Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER),
                Role.restore(UUID.randomUUID(), RoleName.ROLE_ADMIN));
    }

    @Test
    @DisplayName("é igual a si mesmo")
    void igualASiMesmo() {
        Role role = Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER);

        assertEquals(role, role);
    }

    @Test
    @DisplayName("não é igual a nulo nem a objeto de outro tipo")
    void outrosTipos() {
        Role role = Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER);

        assertNotEquals(role, null);
        assertNotEquals(role, "ROLE_OWNER");
    }

    @Test
    @DisplayName("o mesmo papel vindo de dois registros não duplica em um Set")
    void naoDuplicaEmSet() {
        Set<Role> papeis = Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER));

        assertTrue(papeis.contains(Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER)));
    }

    @Test
    @DisplayName("toString devolve o nome do papel")
    void toStringDevolveONome() {
        assertEquals("ROLE_ADMIN", Role.restore(UUID.randomUUID(), RoleName.ROLE_ADMIN).toString());
    }

    @Test
    @DisplayName("RoleName cobre os três papéis do sistema")
    void roleNameCobreOsTresPapeis() {
        assertEquals(3, RoleName.values().length);
        assertEquals(RoleName.ROLE_ADMIN, RoleName.valueOf("ROLE_ADMIN"));
    }
}

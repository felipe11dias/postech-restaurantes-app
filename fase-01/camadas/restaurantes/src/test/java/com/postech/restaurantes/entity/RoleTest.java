package com.postech.restaurantes.entity;

import com.postech.restaurantes.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Role — entidade de papel")
class RoleTest {

    private static final UUID ROLE_ID = UUID.fromString("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee");

    @Test
    @DisplayName("builder preenche id e nome")
    void builder_devePreencherCampos() {
        Role role = Role.builder().id(ROLE_ID).name(RoleName.ROLE_OWNER).build();

        assertThat(role.getId()).isEqualTo(ROLE_ID);
        assertThat(role.getName()).isEqualTo(RoleName.ROLE_OWNER);
    }

    @Test
    @DisplayName("setters alteram o papel criado pelo construtor sem argumentos")
    void setters_deveAlterarCampos() {
        Role role = new Role();

        role.setId(ROLE_ID);
        role.setName(RoleName.ROLE_ADMIN);

        assertThat(role.getId()).isEqualTo(ROLE_ID);
        assertThat(role.getName()).isEqualTo(RoleName.ROLE_ADMIN);
    }
}

package com.postech.restaurantes.entity;

import com.postech.restaurantes.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User — raiz do agregado de usuário")
class UserTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ROLE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");

    @Test
    @DisplayName("builder preenche todos os campos")
    void builder_devePreencherTodosOsCampos() {
        Set<Role> roles = Set.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER));
        List<Address> addresses = List.of(Address.builder().street("Rua A").build());

        User user = User.builder()
                .id(USER_ID)
                .name("João Silva")
                .email("joao@email.com")
                .login("joao.silva")
                .password("HASH")
                .roles(roles)
                .addresses(addresses)
                .build();

        assertThat(user.getId()).isEqualTo(USER_ID);
        assertThat(user.getName()).isEqualTo("João Silva");
        assertThat(user.getEmail()).isEqualTo("joao@email.com");
        assertThat(user.getLogin()).isEqualTo("joao.silva");
        assertThat(user.getPassword()).isEqualTo("HASH");
        assertThat(user.getRoles()).isEqualTo(roles);
        assertThat(user.getAddresses()).isEqualTo(addresses);
    }

    @Test
    @DisplayName("builder sem papéis nem endereços parte de coleções vazias")
    void builder_semAssociacoes_deveIniciarColecoesVazias() {
        User user = User.builder().name("Sem associacoes").build();

        assertThat(user.getRoles()).isEmpty();
        assertThat(user.getAddresses()).isEmpty();
    }

    @Test
    @DisplayName("setters alteram o usuário criado pelo construtor sem argumentos")
    void setters_deveAlterarCampos() {
        User user = new User();
        Set<Role> roles = new HashSet<>(Set.of(new Role(ROLE_ID, RoleName.ROLE_OWNER)));
        List<Address> addresses = new ArrayList<>();

        user.setId(USER_ID);
        user.setName("Maria");
        user.setEmail("maria@email.com");
        user.setLogin("maria");
        user.setPassword("HASH");
        user.setRoles(roles);
        user.setAddresses(addresses);

        assertThat(user.getId()).isEqualTo(USER_ID);
        assertThat(user.getName()).isEqualTo("Maria");
        assertThat(user.getEmail()).isEqualTo("maria@email.com");
        assertThat(user.getLogin()).isEqualTo("maria");
        assertThat(user.getPassword()).isEqualTo("HASH");
        assertThat(user.getRoles()).isSameAs(roles);
        assertThat(user.getAddresses()).isSameAs(addresses);
    }

    @Test
    @DisplayName("addAddress vincula o endereço ao usuário nos dois lados")
    void addAddress_deveVincularOsDoisLados() {
        User user = User.builder().id(USER_ID).build();
        Address address = Address.builder().street("Rua A").build();

        user.addAddress(address);

        assertThat(user.getAddresses()).containsExactly(address);
        assertThat(address.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("removeAddress desfaz o vínculo nos dois lados")
    void removeAddress_deveDesfazerOVinculo() {
        User user = User.builder().id(USER_ID).build();
        Address address = Address.builder().street("Rua A").build();
        user.addAddress(address);

        user.removeAddress(address);

        assertThat(user.getAddresses()).isEmpty();
        assertThat(address.getUserId()).isNull();
    }
}

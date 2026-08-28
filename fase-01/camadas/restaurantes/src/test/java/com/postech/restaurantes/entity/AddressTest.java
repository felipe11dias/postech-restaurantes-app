package com.postech.restaurantes.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Address — entidade de endereço")
class AddressTest {

    private static final UUID ADDRESS_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");

    @Test
    @DisplayName("builder preenche todos os campos")
    void builder_devePreencherTodosOsCampos() {
        Address address = Address.builder()
                .id(ADDRESS_ID)
                .street("Rua das Flores")
                .number("100")
                .complement("Apto 202")
                .neighborhood("Centro")
                .city("Fortaleza")
                .state("CE")
                .zipCode("60175047")
                .userId(USER_ID)
                .build();

        assertThat(address.getId()).isEqualTo(ADDRESS_ID);
        assertThat(address.getStreet()).isEqualTo("Rua das Flores");
        assertThat(address.getNumber()).isEqualTo("100");
        assertThat(address.getComplement()).isEqualTo("Apto 202");
        assertThat(address.getNeighborhood()).isEqualTo("Centro");
        assertThat(address.getCity()).isEqualTo("Fortaleza");
        assertThat(address.getState()).isEqualTo("CE");
        assertThat(address.getZipCode()).isEqualTo("60175047");
        assertThat(address.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("setters alteram o endereço criado pelo construtor sem argumentos")
    void setters_deveAlterarCampos() {
        Address address = new Address();

        address.setId(ADDRESS_ID);
        address.setStreet("Avenida Beira Mar");
        address.setNumber("S/N");
        address.setComplement("Bloco B");
        address.setNeighborhood("Meireles");
        address.setCity("Fortaleza");
        address.setState("CE");
        address.setZipCode("60165121");
        address.setUserId(USER_ID);

        assertThat(address.getId()).isEqualTo(ADDRESS_ID);
        assertThat(address.getStreet()).isEqualTo("Avenida Beira Mar");
        assertThat(address.getNumber()).isEqualTo("S/N");
        assertThat(address.getComplement()).isEqualTo("Bloco B");
        assertThat(address.getNeighborhood()).isEqualTo("Meireles");
        assertThat(address.getCity()).isEqualTo("Fortaleza");
        assertThat(address.getState()).isEqualTo("CE");
        assertThat(address.getZipCode()).isEqualTo("60165121");
        assertThat(address.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("construtor completo preenche todos os campos")
    void construtorCompleto_devePreencherCampos() {
        Address address = new Address(ADDRESS_ID, "Rua A", "10", "Casa", "Bairro",
                "Fortaleza", "CE", "60000000", USER_ID);

        assertThat(address.getStreet()).isEqualTo("Rua A");
        assertThat(address.getUserId()).isEqualTo(USER_ID);
    }
}

package com.postech.restaurantes.mapper;

import com.postech.restaurantes.entity.Address;
import com.postech.restaurantes.vo.v1.request.AddressRequest;
import com.postech.restaurantes.vo.v1.response.AddressResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A implementação do mapper é gerada pelo MapStruct em tempo de compilação; o
 * que se testa aqui é o contrato declarado na interface — em especial os campos
 * marcados como `ignore`, que o Service preenche depois.
 */
@DisplayName("AddressMapper — conversão VO <-> entidade de endereço")
class AddressMapperTest {

    private static final UUID ADDRESS_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");

    private final AddressMapper mapper = new AddressMapperImpl();

    @Test
    @DisplayName("toEntity copia os campos do VO de entrada")
    void toEntity_deveCopiarOsCampos() {
        AddressRequest request = new AddressRequest("Rua das Flores", "100", "Apto 202",
                "Centro", "Fortaleza", "CE", "60175-047");

        Address address = mapper.toEntity(request);

        assertThat(address.getStreet()).isEqualTo("Rua das Flores");
        assertThat(address.getNumber()).isEqualTo("100");
        assertThat(address.getComplement()).isEqualTo("Apto 202");
        assertThat(address.getNeighborhood()).isEqualTo("Centro");
        assertThat(address.getCity()).isEqualTo("Fortaleza");
        assertThat(address.getState()).isEqualTo("CE");
        assertThat(address.getZipCode()).isEqualTo("60175-047");
    }

    /** id e userId são resolvidos na persistência, não no mapeamento. */
    @Test
    @DisplayName("toEntity não preenche id nem userId")
    void toEntity_naoDevePreencherIdNemUserId() {
        Address address = mapper.toEntity(new AddressRequest("Rua A", null, null, null,
                "Fortaleza", "CE", "60175047"));

        assertThat(address.getId()).isNull();
        assertThat(address.getUserId()).isNull();
    }

    @Test
    @DisplayName("toEntity devolve nulo para entrada nula")
    void toEntity_comEntradaNula_deveDevolverNulo() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toResponse copia os campos da entidade")
    void toResponse_deveCopiarOsCampos() {
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

        AddressResponse response = mapper.toResponse(address);

        assertThat(response.id()).isEqualTo(ADDRESS_ID);
        assertThat(response.street()).isEqualTo("Rua das Flores");
        assertThat(response.number()).isEqualTo("100");
        assertThat(response.complement()).isEqualTo("Apto 202");
        assertThat(response.neighborhood()).isEqualTo("Centro");
        assertThat(response.city()).isEqualTo("Fortaleza");
        assertThat(response.state()).isEqualTo("CE");
        assertThat(response.zipCode()).isEqualTo("60175047");
    }

    @Test
    @DisplayName("toResponse devolve nulo para entidade nula")
    void toResponse_comEntidadeNula_deveDevolverNulo() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}

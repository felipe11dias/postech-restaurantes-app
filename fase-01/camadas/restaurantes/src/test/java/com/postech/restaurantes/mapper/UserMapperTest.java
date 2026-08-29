package com.postech.restaurantes.mapper;

import com.postech.restaurantes.entity.Address;
import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.enums.RoleName;
import com.postech.restaurantes.vo.v1.request.AddressRequest;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.vo.v1.response.RoleResponse;
import com.postech.restaurantes.vo.v1.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A implementação é gerada pelo MapStruct; o que se testa aqui é o contrato da
 * interface — os campos marcados como `ignore` (senha e papéis, resolvidos no
 * Service), o mapeamento aninhado dos endereços e o nome do papel como texto.
 */
@DisplayName("UserMapper — conversão VO <-> entidade de usuário")
class UserMapperTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ROLE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID ADDRESS_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private UserMapper mapper;

    /** O mapper aninhado é injetado por campo no código gerado pelo MapStruct. */
    @BeforeEach
    void montarMapper() {
        mapper = new UserMapperImpl();
        ReflectionTestUtils.setField(mapper, "addressMapper", new AddressMapperImpl());
    }

    @Nested
    @DisplayName("toEntity")
    class ParaEntidade {

        @Test
        @DisplayName("copia os dados do cadastro, inclusive os endereços")
        void deveCopiarOsCampos() {
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "João Silva", "joao@email.com", "joao.silva", "senhaSegura123",
                    Set.of(RoleName.ROLE_CUSTOMER),
                    List.of(new AddressRequest("Rua das Flores", "100", null, "Centro",
                            "Fortaleza", "CE", "60175-047")));

            User user = mapper.toEntity(request);

            assertThat(user.getName()).isEqualTo("João Silva");
            assertThat(user.getEmail()).isEqualTo("joao@email.com");
            assertThat(user.getLogin()).isEqualTo("joao.silva");
            assertThat(user.getAddresses()).singleElement()
                    .extracting(Address::getStreet).isEqualTo("Rua das Flores");
        }

        /**
         * Senha e papéis ficam de fora do mapeamento de propósito: a senha recebe
         * hash e os papéis são entidades buscadas no banco — ambos no Service.
         */
        @Test
        @DisplayName("não preenche id, senha nem papéis")
        void naoDevePreencherCamposResolvidosNoService() {
            User user = mapper.toEntity(new UserRegistrationRequest(
                    "João", "joao@email.com", "joao", "senhaSegura123",
                    Set.of(RoleName.ROLE_CUSTOMER), null));

            assertThat(user.getId()).isNull();
            assertThat(user.getPassword()).isNull();
            assertThat(user.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("cadastro sem endereços não gera lista de endereços")
        void semEnderecos_naoDeveGerarLista() {
            User user = mapper.toEntity(new UserRegistrationRequest(
                    "João", "joao@email.com", "joao", "senhaSegura123",
                    Set.of(RoleName.ROLE_CUSTOMER), null));

            assertThat(user.getAddresses()).isNull();
        }

        @Test
        @DisplayName("devolve nulo para entrada nula")
        void comEntradaNula_deveDevolverNulo() {
            assertThat(mapper.toEntity(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ParaResposta {

        @Test
        @DisplayName("copia dados, papéis, endereços e auditoria")
        void deveCopiarOsCampos() {
            LocalDateTime criacao = LocalDateTime.of(2026, 1, 10, 8, 0);
            LocalDateTime atualizacao = LocalDateTime.of(2026, 2, 20, 17, 30);

            User user = User.builder()
                    .id(USER_ID)
                    .name("João Silva")
                    .email("joao@email.com")
                    .login("joao.silva")
                    .password("HASH")
                    .roles(Set.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER)))
                    .addresses(List.of(Address.builder()
                            .id(ADDRESS_ID).street("Rua das Flores").city("Fortaleza")
                            .state("CE").zipCode("60175047").build()))
                    .build();
            user.restoreAudit(criacao, atualizacao, "system", "admin");

            UserResponse response = mapper.toResponse(user);

            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.name()).isEqualTo("João Silva");
            assertThat(response.email()).isEqualTo("joao@email.com");
            assertThat(response.login()).isEqualTo("joao.silva");
            assertThat(response.createdAt()).isEqualTo(criacao);
            assertThat(response.lastUpdatedAt()).isEqualTo(atualizacao);
            assertThat(response.roles()).singleElement()
                    .extracting(RoleResponse::name).isEqualTo("ROLE_CUSTOMER");
            assertThat(response.addresses()).singleElement()
                    .extracting("street").isEqualTo("Rua das Flores");
        }

        @Test
        @DisplayName("usuário sem papéis nem endereços não gera coleções")
        void semAssociacoes_naoDeveGerarColecoes() {
            User user = new User();
            user.setRoles(null);
            user.setAddresses(null);

            UserResponse response = mapper.toResponse(user);

            assertThat(response.roles()).isNull();
            assertThat(response.addresses()).isNull();
        }

        @Test
        @DisplayName("devolve nulo para entidade nula")
        void comEntidadeNula_deveDevolverNulo() {
            assertThat(mapper.toResponse(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toRoleResponse")
    class ParaRespostaDePapel {

        @Test
        @DisplayName("converte o enum do papel para texto")
        void deveConverterONomeParaTexto() {
            RoleResponse response = mapper.toRoleResponse(new Role(ROLE_ID, RoleName.ROLE_ADMIN));

            assertThat(response.id()).isEqualTo(ROLE_ID);
            assertThat(response.name()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("papel sem nome resulta em nome nulo, e não em erro")
        void semNome_deveDevolverNomeNulo() {
            RoleResponse response = mapper.toRoleResponse(new Role(ROLE_ID, null));

            assertThat(response.id()).isEqualTo(ROLE_ID);
            assertThat(response.name()).isNull();
        }

        @Test
        @DisplayName("devolve nulo para papel nulo")
        void comPapelNulo_deveDevolverNulo() {
            assertThat(mapper.toRoleResponse(null)).isNull();
        }
    }
}

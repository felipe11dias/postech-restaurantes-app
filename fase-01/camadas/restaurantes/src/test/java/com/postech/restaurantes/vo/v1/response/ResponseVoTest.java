package com.postech.restaurantes.vo.v1.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VOs de saída (v1) — dados devolvidos pela API")
class ResponseVoTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ROLE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID ADDRESS_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    @Nested
    @DisplayName("AddressResponse")
    class Endereco {

        @Test
        @DisplayName("expõe todos os campos do endereço")
        void deveExporOsCampos() {
            AddressResponse response = new AddressResponse(ADDRESS_ID, "Rua das Flores", "100",
                    "Apto 202", "Centro", "Fortaleza", "CE", "60175047");

            assertThat(response.id()).isEqualTo(ADDRESS_ID);
            assertThat(response.street()).isEqualTo("Rua das Flores");
            assertThat(response.number()).isEqualTo("100");
            assertThat(response.complement()).isEqualTo("Apto 202");
            assertThat(response.neighborhood()).isEqualTo("Centro");
            assertThat(response.city()).isEqualTo("Fortaleza");
            assertThat(response.state()).isEqualTo("CE");
            assertThat(response.zipCode()).isEqualTo("60175047");
        }
    }

    @Nested
    @DisplayName("RoleResponse")
    class Papel {

        @Test
        @DisplayName("expõe id e nome do papel")
        void deveExporOsCampos() {
            RoleResponse response = new RoleResponse(ROLE_ID, "ROLE_CUSTOMER");

            assertThat(response.id()).isEqualTo(ROLE_ID);
            assertThat(response.name()).isEqualTo("ROLE_CUSTOMER");
        }
    }

    @Nested
    @DisplayName("UserResponse")
    class Usuario {

        @Test
        @DisplayName("expõe os dados do usuário, papéis e endereços")
        void deveExporOsCampos() {
            LocalDateTime criacao = LocalDateTime.of(2026, 1, 10, 8, 0);
            LocalDateTime atualizacao = LocalDateTime.of(2026, 2, 20, 17, 30);
            RoleResponse papel = new RoleResponse(ROLE_ID, "ROLE_CUSTOMER");
            AddressResponse endereco = new AddressResponse(ADDRESS_ID, "Rua das Flores", "100",
                    null, "Centro", "Fortaleza", "CE", "60175047");

            UserResponse response = new UserResponse(USER_ID, "João Silva", "joao@email.com",
                    "joao.silva", Set.of(papel), List.of(endereco), criacao, atualizacao);

            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.name()).isEqualTo("João Silva");
            assertThat(response.email()).isEqualTo("joao@email.com");
            assertThat(response.login()).isEqualTo("joao.silva");
            assertThat(response.roles()).containsExactly(papel);
            assertThat(response.addresses()).containsExactly(endereco);
            assertThat(response.createdAt()).isEqualTo(criacao);
            assertThat(response.lastUpdatedAt()).isEqualTo(atualizacao);
        }
    }

    @Nested
    @DisplayName("AuthResponse")
    class Autenticacao {

        @Test
        @DisplayName("a fábrica bearer preenche o tipo do token")
        void bearer_devePreencherOTipo() {
            AuthResponse response = AuthResponse.bearer("jwt-token", 3_600_000L);

            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.type()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(3_600_000L);
        }

        @Test
        @DisplayName("o construtor canônico aceita qualquer tipo de token")
        void construtorCanonico_deveExporOsCampos() {
            AuthResponse response = new AuthResponse("outro-token", "Basic", 60_000L);

            assertThat(response.token()).isEqualTo("outro-token");
            assertThat(response.type()).isEqualTo("Basic");
            assertThat(response.expiresIn()).isEqualTo(60_000L);
        }
    }
}

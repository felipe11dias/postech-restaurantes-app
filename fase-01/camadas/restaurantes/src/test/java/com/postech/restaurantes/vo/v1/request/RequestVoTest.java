package com.postech.restaurantes.vo.v1.request;

import com.postech.restaurantes.enums.RoleName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VOs de entrada da v1. Além de transportar os dados, eles são a primeira
 * barreira de validação da API — o que estes testes verificam junto dos
 * acessores de cada record.
 */
@DisplayName("VOs de entrada (v1) — dados e validação")
class RequestVoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void abrirValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        factory.close();
    }

    private static Set<String> camposInvalidosDe(Object vo) {
        return validator.validate(vo).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static AddressRequest enderecoValido() {
        return new AddressRequest("Rua das Flores", "100", "Apto 202", "Centro",
                "Fortaleza", "CE", "60175-047");
    }

    @Nested
    @DisplayName("AddressRequest")
    class Endereco {

        @Test
        @DisplayName("expõe todos os campos informados")
        void deveExporOsCampos() {
            AddressRequest request = enderecoValido();

            assertThat(request.street()).isEqualTo("Rua das Flores");
            assertThat(request.number()).isEqualTo("100");
            assertThat(request.complement()).isEqualTo("Apto 202");
            assertThat(request.neighborhood()).isEqualTo("Centro");
            assertThat(request.city()).isEqualTo("Fortaleza");
            assertThat(request.state()).isEqualTo("CE");
            assertThat(request.zipCode()).isEqualTo("60175-047");
        }

        @Test
        @DisplayName("endereço completo passa na validação")
        void valido_naoDeveTerViolacoes() {
            assertThat(camposInvalidosDe(enderecoValido())).isEmpty();
        }

        @Test
        @DisplayName("número e complemento são opcionais")
        void numeroEComplemento_saoOpcionais() {
            AddressRequest request = new AddressRequest("Rua das Flores", null, null, null,
                    "Fortaleza", "CE", "60175047");

            assertThat(camposInvalidosDe(request)).isEmpty();
        }

        @Test
        @DisplayName("rua, cidade, estado e CEP em branco são reprovados")
        void camposObrigatoriosEmBranco_deveReprovar() {
            AddressRequest request = new AddressRequest("  ", null, null, null, "", "", "");

            assertThat(camposInvalidosDe(request))
                    .contains("street", "city", "state", "zipCode");
        }

        @Test
        @DisplayName("estado com mais de duas letras é reprovado")
        void estadoForaDaUf_deveReprovar() {
            AddressRequest request = new AddressRequest("Rua das Flores", "100", null, null,
                    "Fortaleza", "Ceará", "60175-047");

            assertThat(camposInvalidosDe(request)).contains("state");
        }

        @Test
        @DisplayName("CEP fora do formato é reprovado")
        void cepForaDoFormato_deveReprovar() {
            AddressRequest request = new AddressRequest("Rua das Flores", "100", null, null,
                    "Fortaleza", "CE", "6017-47");

            assertThat(camposInvalidosDe(request)).contains("zipCode");
        }
    }

    @Nested
    @DisplayName("UserRegistrationRequest")
    class Cadastro {

        @Test
        @DisplayName("expõe todos os campos informados")
        void deveExporOsCampos() {
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "João Silva", "joao@email.com", "joao.silva", "senhaSegura123",
                    Set.of(RoleName.ROLE_CUSTOMER), List.of(enderecoValido()));

            assertThat(request.name()).isEqualTo("João Silva");
            assertThat(request.email()).isEqualTo("joao@email.com");
            assertThat(request.login()).isEqualTo("joao.silva");
            assertThat(request.password()).isEqualTo("senhaSegura123");
            assertThat(request.roles()).containsExactly(RoleName.ROLE_CUSTOMER);
            assertThat(request.addresses()).hasSize(1);
            assertThat(camposInvalidosDe(request)).isEmpty();
        }

        @Test
        @DisplayName("nome, e-mail, login, senha e papéis são obrigatórios")
        void camposObrigatorios_deveReprovar() {
            UserRegistrationRequest request = new UserRegistrationRequest(
                    " ", " ", " ", " ", Set.of(), null);

            assertThat(camposInvalidosDe(request))
                    .contains("name", "email", "login", "password", "roles");
        }

        @Test
        @DisplayName("senha com menos de 8 caracteres é reprovada")
        void senhaCurta_deveReprovar() {
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "João", "joao@email.com", "joao", "curta",
                    Set.of(RoleName.ROLE_CUSTOMER), null);

            assertThat(camposInvalidosDe(request)).contains("password");
        }

        /** O @Valid na lista faz a validação descer até cada endereço. */
        @Test
        @DisplayName("endereço inválido na lista reprova o cadastro inteiro")
        void enderecoInvalidoNaLista_deveReprovar() {
            UserRegistrationRequest request = new UserRegistrationRequest(
                    "João", "joao@email.com", "joao", "senhaSegura123",
                    Set.of(RoleName.ROLE_CUSTOMER),
                    List.of(new AddressRequest("", null, null, null, "", "", "")));

            assertThat(camposInvalidosDe(request)).contains("addresses[0].street");
        }
    }

    @Nested
    @DisplayName("UserUpdateRequest")
    class Atualizacao {

        @Test
        @DisplayName("expõe todos os campos informados")
        void deveExporOsCampos() {
            UserUpdateRequest request = new UserUpdateRequest(
                    "João Silva", "joao@email.com", "joao.silva", List.of(enderecoValido()));

            assertThat(request.name()).isEqualTo("João Silva");
            assertThat(request.email()).isEqualTo("joao@email.com");
            assertThat(request.login()).isEqualTo("joao.silva");
            assertThat(request.addresses()).hasSize(1);
            assertThat(camposInvalidosDe(request)).isEmpty();
        }

        @Test
        @DisplayName("nome, e-mail e login são obrigatórios")
        void camposObrigatorios_deveReprovar() {
            UserUpdateRequest request = new UserUpdateRequest(" ", "nao-e-email", " ", null);

            assertThat(camposInvalidosDe(request)).contains("name", "email", "login");
        }
    }

    @Nested
    @DisplayName("LoginRequest")
    class Login {

        @Test
        @DisplayName("expõe login e senha")
        void deveExporOsCampos() {
            LoginRequest request = new LoginRequest("joao.silva", "senhaSegura123");

            assertThat(request.login()).isEqualTo("joao.silva");
            assertThat(request.password()).isEqualTo("senhaSegura123");
            assertThat(camposInvalidosDe(request)).isEmpty();
        }

        @Test
        @DisplayName("login e senha em branco são reprovados")
        void camposEmBranco_deveReprovar() {
            assertThat(camposInvalidosDe(new LoginRequest(" ", " ")))
                    .containsExactlyInAnyOrder("login", "password");
        }
    }

    @Nested
    @DisplayName("PasswordChangeRequest")
    class TrocaDeSenha {

        @Test
        @DisplayName("expõe as três senhas")
        void deveExporOsCampos() {
            PasswordChangeRequest request =
                    new PasswordChangeRequest("atual123", "nova12345", "nova12345");

            assertThat(request.currentPassword()).isEqualTo("atual123");
            assertThat(request.newPassword()).isEqualTo("nova12345");
            assertThat(request.confirmPassword()).isEqualTo("nova12345");
            assertThat(camposInvalidosDe(request)).isEmpty();
        }

        @Test
        @DisplayName("nova senha com menos de 8 caracteres é reprovada")
        void novaSenhaCurta_deveReprovar() {
            assertThat(camposInvalidosDe(new PasswordChangeRequest("atual123", "curta", "curta")))
                    .contains("newPassword");
        }
    }

    @Nested
    @DisplayName("ForgotPasswordRequest e ResetPasswordRequest")
    class RecuperacaoDeSenha {

        @Test
        @DisplayName("ForgotPasswordRequest expõe o e-mail")
        void forgot_deveExporOEmail() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("joao@email.com");

            assertThat(request.email()).isEqualTo("joao@email.com");
            assertThat(camposInvalidosDe(request)).isEmpty();
        }

        @Test
        @DisplayName("ForgotPasswordRequest reprova e-mail fora do formato")
        void forgot_emailInvalido_deveReprovar() {
            assertThat(camposInvalidosDe(new ForgotPasswordRequest("nao-e-email"))).contains("email");
        }

        @Test
        @DisplayName("ResetPasswordRequest expõe token e senhas")
        void reset_deveExporOsCampos() {
            ResetPasswordRequest request =
                    new ResetPasswordRequest("TOKEN-OPACO", "nova12345", "nova12345");

            assertThat(request.token()).isEqualTo("TOKEN-OPACO");
            assertThat(request.newPassword()).isEqualTo("nova12345");
            assertThat(request.confirmPassword()).isEqualTo("nova12345");
            assertThat(camposInvalidosDe(request)).isEmpty();
        }

        @Test
        @DisplayName("ResetPasswordRequest exige token e nova senha com 8 caracteres")
        void reset_camposInvalidos_deveReprovar() {
            assertThat(camposInvalidosDe(new ResetPasswordRequest(" ", "curta", " ")))
                    .contains("token", "newPassword", "confirmPassword");
        }
    }
}

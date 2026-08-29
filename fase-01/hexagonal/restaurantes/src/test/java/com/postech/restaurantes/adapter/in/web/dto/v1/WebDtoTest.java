package com.postech.restaurantes.adapter.in.web.dto.v1;

import com.postech.restaurantes.adapter.in.web.dto.v1.request.AddressRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.ForgotPasswordRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.LoginRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.ResetPasswordRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.UserUpdateRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.AddressResponse;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.AuthResponse;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.RoleResponse;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.UserResponse;
import com.postech.restaurantes.application.port.in.command.AddressCommand;
import com.postech.restaurantes.application.port.in.command.ChangePasswordCommand;
import com.postech.restaurantes.application.port.in.command.RegisterUserCommand;
import com.postech.restaurantes.application.port.in.command.UpdateUserCommand;
import com.postech.restaurantes.application.port.in.view.AddressView;
import com.postech.restaurantes.application.port.in.view.AuthView;
import com.postech.restaurantes.application.port.in.view.RoleView;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.domain.model.RoleName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os DTOs são a fronteira do adapter web: a Bean Validation é a primeira barreira
 * de formato, e o {@code toCommand}/{@code from} é o ponto onde o vocabulário HTTP
 * vira vocabulário da aplicação. Daqui para dentro ninguém mais conhece estes
 * records — é isso que permite versionar o contrato sem tocar no caso de uso.
 */
@DisplayName("DTOs do adapter web (v1)")
class WebDtoTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

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

    private static Set<String> camposInvalidosDe(Object dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static AddressRequest enderecoValido() {
        return new AddressRequest("Av. Paulista", "1500", "Apto 42", "Bela Vista",
                "São Paulo", "SP", "01310-200");
    }

    @Nested
    @DisplayName("AddressRequest")
    class Endereco {

        @Test
        @DisplayName("expõe os campos e vira um AddressCommand equivalente")
        void viraCommand() {
            AddressRequest request = enderecoValido();

            assertEquals("Av. Paulista", request.street());
            assertEquals("1500", request.number());
            assertEquals("Apto 42", request.complement());
            assertEquals("Bela Vista", request.neighborhood());
            assertEquals("São Paulo", request.city());
            assertEquals("SP", request.state());
            assertEquals("01310-200", request.zipCode());

            AddressCommand command = request.toCommand();
            assertEquals("Av. Paulista", command.street());
            assertEquals("1500", command.number());
            assertEquals("Apto 42", command.complement());
            assertEquals("Bela Vista", command.neighborhood());
            assertEquals("São Paulo", command.city());
            assertEquals("SP", command.state());
            assertEquals("01310-200", command.zipCode());
            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("número, complemento e bairro são opcionais")
        void camposOpcionais() {
            AddressRequest request = new AddressRequest("Av. Paulista", null, null, null,
                    "São Paulo", "SP", "01310200");

            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("rua, cidade, estado e CEP em branco são reprovados")
        void obrigatoriosEmBranco() {
            Set<String> invalidos = camposInvalidosDe(
                    new AddressRequest("  ", null, null, null, "", "", ""));

            assertTrue(invalidos.containsAll(Set.of("street", "city", "state", "zipCode")));
        }

        @Test
        @DisplayName("estado fora da UF de duas letras é reprovado")
        void estadoForaDaUf() {
            assertTrue(camposInvalidosDe(new AddressRequest("Av. Paulista", "1500", null, null,
                    "São Paulo", "São Paulo", "01310-200")).contains("state"));
        }

        @Test
        @DisplayName("CEP fora do formato é reprovado")
        void cepForaDoFormato() {
            assertTrue(camposInvalidosDe(new AddressRequest("Av. Paulista", "1500", null, null,
                    "São Paulo", "SP", "0131-20")).contains("zipCode"));
        }
    }

    @Nested
    @DisplayName("UserRegistrationRequest")
    class Cadastro {

        private UserRegistrationRequest valido(List<AddressRequest> enderecos) {
            return new UserRegistrationRequest("Maria Silva", "maria@email.com", "maria.silva",
                    "senha12345", Set.of(RoleName.ROLE_CUSTOMER), enderecos);
        }

        @Test
        @DisplayName("expõe os campos e vira um RegisterUserCommand equivalente")
        void viraCommand() {
            UserRegistrationRequest request = valido(List.of(enderecoValido()));

            assertEquals("Maria Silva", request.name());
            assertEquals("maria@email.com", request.email());
            assertEquals("maria.silva", request.login());
            assertEquals("senha12345", request.password());
            assertEquals(Set.of(RoleName.ROLE_CUSTOMER), request.roles());
            assertEquals(1, request.addresses().size());

            RegisterUserCommand command = request.toCommand();
            assertEquals("Maria Silva", command.name());
            assertEquals("maria@email.com", command.email());
            assertEquals("maria.silva", command.login());
            assertEquals("senha12345", command.rawPassword());
            assertEquals(Set.of(RoleName.ROLE_CUSTOMER), command.roles());
            assertEquals(1, command.addresses().size());
            assertEquals("Av. Paulista", command.addresses().get(0).street());
            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("sem endereços, o command leva uma lista vazia")
        void semEnderecos() {
            assertTrue(valido(null).toCommand().addresses().isEmpty());
        }

        @Test
        @DisplayName("nome, e-mail, login, senha e papéis são obrigatórios")
        void obrigatorios() {
            Set<String> invalidos = camposInvalidosDe(new UserRegistrationRequest(
                    " ", " ", " ", " ", Set.of(), null));

            assertTrue(invalidos.containsAll(Set.of("name", "email", "login", "password", "roles")));
        }

        @Test
        @DisplayName("senha com menos de 8 caracteres é reprovada")
        void senhaCurta() {
            assertTrue(camposInvalidosDe(new UserRegistrationRequest("Maria", "maria@email.com",
                    "maria", "curta", Set.of(RoleName.ROLE_CUSTOMER), null)).contains("password"));
        }

        /** O @Valid na lista faz a validação descer até cada endereço. */
        @Test
        @DisplayName("endereço inválido na lista reprova o cadastro inteiro")
        void enderecoInvalidoNaLista() {
            UserRegistrationRequest request = valido(
                    List.of(new AddressRequest("", null, null, null, "", "", "")));

            assertTrue(camposInvalidosDe(request).contains("addresses[0].street"));
        }
    }

    @Nested
    @DisplayName("UserUpdateRequest")
    class Atualizacao {

        /** O id vem do path, não do corpo: o recurso alterado é o da URL. */
        @Test
        @DisplayName("o id do command vem do path, não do corpo")
        void idVemDoPath() {
            UserUpdateRequest request = new UserUpdateRequest("Maria Souza",
                    "maria.souza@email.com", "maria.souza", List.of(enderecoValido()));

            UpdateUserCommand command = request.toCommand(USER_ID);

            assertEquals(USER_ID, command.userId());
            assertEquals("Maria Souza", command.name());
            assertEquals("maria.souza@email.com", command.email());
            assertEquals("maria.souza", command.login());
            assertEquals(1, command.addresses().size());
            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("nome, e-mail e login são obrigatórios")
        void obrigatorios() {
            Set<String> invalidos =
                    camposInvalidosDe(new UserUpdateRequest(" ", "nao-e-email", " ", null));

            assertTrue(invalidos.containsAll(Set.of("name", "email", "login")));
        }

        @Test
        @DisplayName("sem endereços, o command leva uma lista vazia")
        void semEnderecos() {
            UserUpdateRequest request =
                    new UserUpdateRequest("Maria", "maria@email.com", "maria", null);

            assertTrue(request.toCommand(USER_ID).addresses().isEmpty());
        }
    }

    @Nested
    @DisplayName("DTOs de autenticação e senha")
    class Autenticacao {

        @Test
        @DisplayName("LoginRequest vira um AuthenticateCommand")
        void login() {
            LoginRequest request = new LoginRequest("maria.silva", "senha12345");

            assertEquals("maria.silva", request.login());
            assertEquals("senha12345", request.password());
            assertEquals("maria.silva", request.toCommand().login());
            assertEquals("senha12345", request.toCommand().rawPassword());
            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("LoginRequest exige login e senha")
        void loginObrigatorio() {
            assertEquals(Set.of("login", "password"), camposInvalidosDe(new LoginRequest(" ", " ")));
        }

        @Test
        @DisplayName("PasswordChangeRequest vira um ChangePasswordCommand com o id do path")
        void trocaDeSenha() {
            PasswordChangeRequest request =
                    new PasswordChangeRequest("atual123", "nova12345", "nova12345");

            assertEquals("atual123", request.currentPassword());
            assertEquals("nova12345", request.newPassword());
            assertEquals("nova12345", request.confirmPassword());

            ChangePasswordCommand command = request.toCommand(USER_ID);
            assertEquals(USER_ID, command.userId());
            assertEquals("atual123", command.currentPassword());
            assertEquals("nova12345", command.newPassword());
            assertEquals("nova12345", command.confirmPassword());
            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("PasswordChangeRequest exige nova senha com ao menos 8 caracteres")
        void novaSenhaCurta() {
            assertTrue(camposInvalidosDe(
                    new PasswordChangeRequest("atual123", "curta", "curta")).contains("newPassword"));
        }

        @Test
        @DisplayName("ForgotPasswordRequest vira um RequestPasswordResetCommand")
        void esqueciASenha() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("maria@email.com");

            assertEquals("maria@email.com", request.email());
            assertEquals("maria@email.com", request.toCommand().email());
            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("ForgotPasswordRequest reprova e-mail fora do formato")
        void emailInvalido() {
            assertTrue(camposInvalidosDe(new ForgotPasswordRequest("nao-e-email")).contains("email"));
        }

        @Test
        @DisplayName("ResetPasswordRequest vira um ResetPasswordCommand")
        void redefinirSenha() {
            ResetPasswordRequest request =
                    new ResetPasswordRequest("TOKEN-OPACO", "nova12345", "nova12345");

            assertEquals("TOKEN-OPACO", request.token());
            assertEquals("nova12345", request.newPassword());
            assertEquals("nova12345", request.confirmPassword());
            assertEquals("TOKEN-OPACO", request.toCommand().rawToken());
            assertEquals("nova12345", request.toCommand().newPassword());
            assertEquals("nova12345", request.toCommand().confirmPassword());
            assertTrue(camposInvalidosDe(request).isEmpty());
        }

        @Test
        @DisplayName("ResetPasswordRequest exige token e nova senha de 8 caracteres")
        void redefinirInvalido() {
            Set<String> invalidos =
                    camposInvalidosDe(new ResetPasswordRequest(" ", "curta", " "));

            assertTrue(invalidos.containsAll(Set.of("token", "newPassword", "confirmPassword")));
        }
    }

    @Nested
    @DisplayName("DTOs de saída")
    class Saida {

        @Test
        @DisplayName("AddressResponse é montado a partir da AddressView")
        void endereco() {
            AddressView view = new AddressView(ADDRESS_ID, "Av. Paulista", "1500", "Apto 42",
                    "Bela Vista", "São Paulo", "SP", "01310-200");

            AddressResponse response = AddressResponse.from(view);

            assertEquals(ADDRESS_ID, response.id());
            assertEquals("Av. Paulista", response.street());
            assertEquals("1500", response.number());
            assertEquals("Apto 42", response.complement());
            assertEquals("Bela Vista", response.neighborhood());
            assertEquals("São Paulo", response.city());
            assertEquals("SP", response.state());
            assertEquals("01310-200", response.zipCode());
        }

        @Test
        @DisplayName("RoleResponse é montado a partir da RoleView")
        void papel() {
            RoleResponse response = RoleResponse.from(new RoleView(ROLE_ID, "ROLE_CUSTOMER"));

            assertEquals(ROLE_ID, response.id());
            assertEquals("ROLE_CUSTOMER", response.name());
        }

        @Test
        @DisplayName("AuthResponse é montado a partir da AuthView")
        void autenticacao() {
            AuthResponse response = AuthResponse.from(AuthView.bearer("jwt-token", 3_600_000L));

            assertEquals("jwt-token", response.token());
            assertEquals("Bearer", response.type());
            assertEquals(3_600_000L, response.expiresIn());
        }

        /** A senha não aparece aqui — e nem teria como: a UserView também não a carrega. */
        @Test
        @DisplayName("UserResponse é montado a partir da UserView, com papéis e endereços")
        void usuario() {
            LocalDateTime criacao = LocalDateTime.of(2026, 1, 10, 8, 0);
            LocalDateTime atualizacao = LocalDateTime.of(2026, 2, 20, 17, 30);
            UserView view = new UserView(USER_ID, "Maria Silva", "maria@email.com", "maria.silva",
                    Set.of(new RoleView(ROLE_ID, "ROLE_CUSTOMER")),
                    List.of(new AddressView(ADDRESS_ID, "Av. Paulista", "1500", null,
                            "Bela Vista", "São Paulo", "SP", "01310-200")),
                    criacao, atualizacao);

            UserResponse response = UserResponse.from(view);

            assertEquals(USER_ID, response.id());
            assertEquals("Maria Silva", response.name());
            assertEquals("maria@email.com", response.email());
            assertEquals("maria.silva", response.login());
            assertEquals(criacao, response.createdAt());
            assertEquals(atualizacao, response.lastUpdatedAt());
            assertEquals(1, response.roles().size());
            assertEquals("ROLE_CUSTOMER", response.roles().iterator().next().name());
            assertEquals(1, response.addresses().size());
            assertEquals("Av. Paulista", response.addresses().get(0).street());
        }
    }
}

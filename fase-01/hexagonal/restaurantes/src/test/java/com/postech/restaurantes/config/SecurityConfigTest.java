package com.postech.restaurantes.config;

import com.postech.restaurantes.adapter.in.web.UserRestController;
import com.postech.restaurantes.adapter.in.web.assembler.UserModelAssembler;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.UserResponse;
import com.postech.restaurantes.adapter.in.web.security.JwtAuthenticationEntryPoint;
import com.postech.restaurantes.adapter.in.web.security.JwtAuthenticationFilter;
import com.postech.restaurantes.adapter.in.web.security.ResourceOwnerChecker;
import com.postech.restaurantes.application.port.in.ChangePasswordUseCase;
import com.postech.restaurantes.application.port.in.DeleteUserUseCase;
import com.postech.restaurantes.application.port.in.FindUserUseCase;
import com.postech.restaurantes.application.port.in.RegisterUserUseCase;
import com.postech.restaurantes.application.port.in.UpdateUserUseCase;
import com.postech.restaurantes.application.port.in.command.RegisterUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.application.port.out.TokenVerifierPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As regras de acesso são configuração, não código de negócio: só uma requisição
 * de verdade atravessando a cadeia de filtros comprova que os endpoints públicos
 * seguem abertos e que os demais exigem token.
 */
@WebMvcTest(controllers = UserRestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@DisplayName("SecurityConfig — regras de acesso da API")
class SecurityConfigTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private RegisterUserUseCase registerUserUseCase;
    @MockitoBean private UpdateUserUseCase updateUserUseCase;
    @MockitoBean private ChangePasswordUseCase changePasswordUseCase;
    @MockitoBean private DeleteUserUseCase deleteUserUseCase;
    @MockitoBean private FindUserUseCase findUserUseCase;
    @MockitoBean private UserModelAssembler assembler;
    @MockitoBean private TokenVerifierPort tokenVerifierPort;
    @MockitoBean private ResourceOwnerChecker resourceOwner;

    private UserView view() {
        return new UserView(USER_ID, "Maria Silva", "maria@email.com", "maria.silva",
                Set.of(), List.of(), null, null);
    }

    /** Autocadastro é público: é por ele que o primeiro usuário entra no sistema. */
    @Test
    @DisplayName("o autocadastro (POST /api/v1/users) é público")
    void autocadastroEhPublico() throws Exception {
        given(registerUserUseCase.register(any(RegisterUserCommand.class))).willReturn(view());
        given(assembler.toModel(any(UserResponse.class))).willReturn(
                EntityModel.of(UserResponse.from(view()),
                        Link.of("http://localhost/api/v1/users/" + USER_ID).withSelfRel()));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maria Silva",
                                  "email": "maria@email.com",
                                  "login": "maria.silva",
                                  "password": "senha12345",
                                  "roles": ["ROLE_CUSTOMER"]
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("consulta de usuário sem token responde 401 no padrão ProblemDetail")
    void consultaSemToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", USER_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.type").value("/problemas/nao-autenticado"));
    }

    @Test
    @DisplayName("listagem de usuários sem token responde 401")
    void listagemSemToken() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token recusado pelo verificador não autentica a requisição")
    void tokenRecusado() throws Exception {
        given(tokenVerifierPort.verify("token-ruim")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/{id}", USER_ID)
                        .header("Authorization", "Bearer token-ruim"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("o encoder de senha publicado é o BCrypt")
    void encoderEhBCrypt() {
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);

        String hash = passwordEncoder.encode("senha12345");
        assertNotEquals("senha12345", hash);
        assertTrue(passwordEncoder.matches("senha12345", hash));
        assertFalse(passwordEncoder.matches("senha-errada", hash));
    }
}

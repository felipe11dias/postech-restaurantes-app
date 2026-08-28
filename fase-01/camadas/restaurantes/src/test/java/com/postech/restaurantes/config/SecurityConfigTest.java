package com.postech.restaurantes.config;

import com.postech.restaurantes.assembler.UserModelAssembler;
import com.postech.restaurantes.controller.UserController;
import com.postech.restaurantes.security.CustomUserDetailsService;
import com.postech.restaurantes.security.JwtAuthenticationEntryPoint;
import com.postech.restaurantes.security.JwtAuthenticationFilter;
import com.postech.restaurantes.security.JwtService;
import com.postech.restaurantes.security.UserSecurity;
import com.postech.restaurantes.service.UserService;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.vo.v1.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As regras de acesso são configuração, não código de negócio: só uma requisição
 * de verdade atravessando a cadeia de filtros comprova que os endpoints públicos
 * seguem abertos e que os demais exigem token.
 */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@DisplayName("SecurityConfig — regras de acesso da API")
class SecurityConfigTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;

    @MockitoBean private UserService userService;
    @MockitoBean private UserModelAssembler assembler;
    @MockitoBean private UserSecurity userSecurity;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    private UserResponse usuario() {
        return new UserResponse(USER_ID, "João Silva", "joao@email.com", "joao.silva",
                Set.of(), List.of(), null, null);
    }

    /** Auto-cadastro é público: é por ele que o primeiro usuário entra no sistema. */
    @Test
    @DisplayName("o auto-cadastro (POST /api/v1/users) é público")
    void autoCadastro_deveSerPublico() throws Exception {
        when(userService.register(any(UserRegistrationRequest.class))).thenReturn(usuario());
        when(assembler.toModel(any(UserResponse.class))).thenReturn(EntityModel.of(usuario(),
                Link.of("http://localhost/api/v1/users/" + USER_ID).withSelfRel()));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "João Silva",
                                  "email": "joao@email.com",
                                  "login": "joao.silva",
                                  "password": "senhaSegura123",
                                  "roles": ["ROLE_CUSTOMER"]
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("consulta de usuário sem token responde 401 no padrão ProblemDetail")
    void consultaSemToken_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", USER_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.type").value("/problemas/nao-autenticado"));
    }

    @Test
    @DisplayName("listagem de usuários sem token responde 401")
    void listagemSemToken_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token inválido no cabeçalho não autentica a requisição")
    void tokenInvalido_deveResponder401() throws Exception {
        when(jwtService.extractUsername("token-corrompido"))
                .thenThrow(new IllegalArgumentException("token corrompido"));

        mockMvc.perform(get("/api/v1/users/{id}", USER_ID)
                        .header("Authorization", "Bearer token-corrompido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("o encoder de senha publicado é o BCrypt")
    void passwordEncoder_deveSerBCrypt() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);

        String hash = passwordEncoder.encode("senhaSegura123");
        assertThat(hash).isNotEqualTo("senhaSegura123");
        assertThat(passwordEncoder.matches("senhaSegura123", hash)).isTrue();
        assertThat(passwordEncoder.matches("senha-errada", hash)).isFalse();
    }

    @Test
    @DisplayName("o AuthenticationManager é publicado como bean")
    void authenticationManager_devEstarDisponivel() {
        assertThat(authenticationManager).isNotNull();
    }
}

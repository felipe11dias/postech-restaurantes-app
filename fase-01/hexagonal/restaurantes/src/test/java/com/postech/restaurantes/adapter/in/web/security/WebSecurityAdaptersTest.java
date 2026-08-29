package com.postech.restaurantes.adapter.in.web.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postech.restaurantes.adapter.in.web.exception.ProblemType;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.TokenVerifierPort;
import com.postech.restaurantes.application.port.out.TokenVerifierPort.AuthenticatedPrincipal;
import com.postech.restaurantes.domain.DomainFixtures;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** As três peças de segurança do adapter de entrada web. */
class WebSecurityAdaptersTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("JwtAuthenticationFilter")
    class Filtro {

        @Mock
        private TokenVerifierPort tokenVerifierPort;
        @Mock
        private FilterChain filterChain;

        @InjectMocks
        private JwtAuthenticationFilter filter;

        private final MockHttpServletRequest request = new MockHttpServletRequest();
        private final MockHttpServletResponse response = new MockHttpServletResponse();

        @AfterEach
        void limparContexto() {
            SecurityContextHolder.clearContext();
        }

        private Authentication autenticacaoAtual() {
            return SecurityContextHolder.getContext().getAuthentication();
        }

        @Test
        @DisplayName("token válido autentica o requisitante com os papéis do token")
        void tokenValido() throws Exception {
            request.addHeader("Authorization", "Bearer token-valido");
            given(tokenVerifierPort.verify("token-valido")).willReturn(
                    Optional.of(new AuthenticatedPrincipal("maria.silva", Set.of("ROLE_CUSTOMER"))));

            filter.doFilterInternal(request, response, filterChain);

            assertEquals("maria.silva", autenticacaoAtual().getName());
            assertEquals(List.of("ROLE_CUSTOMER"), autenticacaoAtual().getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList());
            assertInstanceOf(WebAuthenticationDetails.class, autenticacaoAtual().getDetails());
            verify(filterChain).doFilter(request, response);
        }

        /** O filtro nunca barra a requisição: quem decide o acesso é a cadeia. */
        @Test
        @DisplayName("token inválido segue sem autenticar, sem interromper a cadeia")
        void tokenInvalido() throws Exception {
            request.addHeader("Authorization", "Bearer token-ruim");
            given(tokenVerifierPort.verify("token-ruim")).willReturn(Optional.empty());

            filter.doFilterInternal(request, response, filterChain);

            assertNull(autenticacaoAtual());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("requisição sem cabeçalho Authorization segue sem autenticar")
        void semCabecalho() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertNull(autenticacaoAtual());
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenVerifierPort);
        }

        @Test
        @DisplayName("cabeçalho em outro esquema que não Bearer segue sem autenticar")
        void outroEsquema() throws Exception {
            request.addHeader("Authorization", "Basic bWFyaWE6c2VuaGE=");

            filter.doFilterInternal(request, response, filterChain);

            assertNull(autenticacaoAtual());
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenVerifierPort);
        }

        @Test
        @DisplayName("contexto já autenticado é preservado")
        void contextoJaAutenticado() throws Exception {
            Authentication existente =
                    new UsernamePasswordAuthenticationToken("outro.usuario", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(existente);
            request.addHeader("Authorization", "Bearer token-valido");

            filter.doFilterInternal(request, response, filterChain);

            assertEquals(existente, autenticacaoAtual());
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenVerifierPort);
        }
    }

    @Nested
    @DisplayName("JwtAuthenticationEntryPoint")
    class PontoDeEntrada {

        private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        private final JwtAuthenticationEntryPoint entryPoint =
                new JwtAuthenticationEntryPoint(objectMapper);

        /**
         * Requisições barradas pela cadeia de filtros não chegam ao
         * {@code @RestControllerAdvice}; sem este ponto de entrada, o 401 sairia no
         * formato padrão do container e quebraria a consistência do corpo de erro.
         */
        @Test
        @DisplayName("responde 401 no mesmo padrão ProblemDetail do resto da API")
        void respondeProblemDetail() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();

            entryPoint.commence(new MockHttpServletRequest("GET", "/api/v1/users"), response,
                    new InsufficientAuthenticationException("Full authentication is required"));

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, response.getContentType());

            JsonNode corpo = objectMapper.readTree(response.getContentAsByteArray());
            assertEquals(HttpStatus.UNAUTHORIZED.value(), corpo.get("status").asInt());
            assertEquals(ProblemType.NAO_AUTENTICADO, corpo.get("type").asText());
            assertEquals("Não autenticado", corpo.get("title").asText());
            assertEquals("Autenticação necessária para acessar este recurso",
                    corpo.get("detail").asText());
            assertTrue(corpo.has("timestamp"));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("ResourceOwnerChecker")
    class DonoDoRecurso {

        private static final UUID USER_ID = UUID.randomUUID();

        @Mock
        private LoadUserPort loadUserPort;

        @InjectMocks
        private ResourceOwnerChecker checker;

        private Authentication autenticado(String login) {
            return new UsernamePasswordAuthenticationToken(login, null, List.of());
        }

        @Test
        @DisplayName("o dono do registro é reconhecido")
        void dono() {
            given(loadUserPort.findByLogin("maria.silva"))
                    .willReturn(Optional.of(DomainFixtures.usuarioPersistido(USER_ID)));

            assertTrue(checker.isSelf(USER_ID, autenticado("maria.silva")));
        }

        @Test
        @DisplayName("o registro de outro usuário é recusado")
        void deOutro() {
            given(loadUserPort.findByLogin("maria.silva"))
                    .willReturn(Optional.of(DomainFixtures.usuarioPersistido(UUID.randomUUID())));

            assertFalse(checker.isSelf(USER_ID, autenticado("maria.silva")));
        }

        @Test
        @DisplayName("login autenticado sem usuário correspondente é recusado")
        void semUsuario() {
            given(loadUserPort.findByLogin("fantasma")).willReturn(Optional.empty());

            assertFalse(checker.isSelf(USER_ID, autenticado("fantasma")));
        }

        @Test
        @DisplayName("id nulo é recusado sem consultar o repositório")
        void idNulo() {
            assertFalse(checker.isSelf(null, autenticado("maria.silva")));

            verifyNoInteractions(loadUserPort);
        }

        @Test
        @DisplayName("requisição sem autenticação é recusada sem consultar o repositório")
        void semAutenticacao() {
            assertFalse(checker.isSelf(USER_ID, null));

            verifyNoInteractions(loadUserPort);
        }

        @Test
        @DisplayName("autenticação ainda não confirmada é recusada")
        void naoConfirmada() {
            assertFalse(checker.isSelf(USER_ID,
                    new UsernamePasswordAuthenticationToken("maria.silva", "senha")));

            verify(loadUserPort, org.mockito.Mockito.never()).findByLogin(anyString());
        }
    }
}

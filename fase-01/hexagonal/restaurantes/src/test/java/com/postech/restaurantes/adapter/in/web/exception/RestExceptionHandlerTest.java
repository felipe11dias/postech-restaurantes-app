package com.postech.restaurantes.adapter.in.web.exception;

import com.postech.restaurantes.domain.exception.AuthenticationFailedException;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Esta classe é a materialização de uma fronteira: o domínio lança
 * {@code DuplicateResourceException} sem saber que isso é um 409. Cada teste fixa
 * um par (exceção do núcleo, resposta HTTP) — e o {@code type} de cada categoria,
 * que é o contrato programático do cliente, já que o {@code title} é texto livre
 * em português e pode ser reescrito.
 */
@DisplayName("RestExceptionHandler — tradução das exceções para HTTP")
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Nested
    @DisplayName("exceções do núcleo")
    class DoNucleo {

        @Test
        @DisplayName("recurso não encontrado vira 404")
        void naoEncontrado() {
            UUID id = UUID.randomUUID();

            ProblemDetail problem = handler.handleNotFound(ResourceNotFoundException.user(id));

            assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
            assertEquals("Recurso não encontrado", problem.getTitle());
            assertEquals(URI.create(ProblemType.RECURSO_NAO_ENCONTRADO), problem.getType());
            assertTrue(problem.getDetail().contains(id.toString()));
        }

        @Test
        @DisplayName("valor único duplicado vira 409")
        void duplicado() {
            ProblemDetail problem =
                    handler.handleDuplicate(DuplicateResourceException.email("maria@email.com"));

            assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
            assertEquals("Conflito de dados", problem.getTitle());
            assertEquals(URI.create(ProblemType.CONFLITO_DE_DADOS), problem.getType());
            assertTrue(problem.getDetail().contains("maria@email.com"));
        }

        @Test
        @DisplayName("senha inválida vira 400")
        void senhaInvalida() {
            ProblemDetail problem = handler.handleInvalidPassword(
                    InvalidPasswordException.currentPasswordMismatch());

            assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
            assertEquals("Senha inválida", problem.getTitle());
            assertEquals(URI.create(ProblemType.SENHA_INVALIDA), problem.getType());
        }

        @Test
        @DisplayName("token de redefinição inválido ou expirado vira 400")
        void tokenInvalido() {
            ProblemDetail problem = handler.handleInvalidOrExpiredToken(
                    new InvalidOrExpiredTokenException());

            assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
            assertEquals("Token inválido ou expirado", problem.getTitle());
            assertEquals(URI.create(ProblemType.TOKEN_INVALIDO_OU_EXPIRADO), problem.getType());
        }

        @Test
        @DisplayName("operação vetada por regra de negócio vira 403")
        void operacaoVetada() {
            ProblemDetail problem = handler.handleForbiddenOperation(
                    ForbiddenOperationException.selfRegistrationWithRole(RoleName.ROLE_ADMIN));

            assertEquals(HttpStatus.FORBIDDEN.value(), problem.getStatus());
            assertEquals("Operação não permitida", problem.getTitle());
            assertEquals(URI.create(ProblemType.OPERACAO_NAO_PERMITIDA), problem.getType());
            assertTrue(problem.getDetail().contains("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("credenciais recusadas pelo caso de uso viram 401")
        void credenciaisRecusadas() {
            ProblemDetail problem = handler.handleAuthenticationFailed(
                    new AuthenticationFailedException());

            assertEquals(HttpStatus.UNAUTHORIZED.value(), problem.getStatus());
            assertEquals("Falha na autenticação", problem.getTitle());
            assertEquals(URI.create(ProblemType.FALHA_AUTENTICACAO), problem.getType());
        }

        @Test
        @DisplayName("invariante de Value Object violada vira 400 com a mensagem do domínio")
        void invarianteViolada() {
            ProblemDetail problem = handler.handleIllegalArgument(
                    new IllegalArgumentException("O nome é obrigatório"));

            assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
            assertEquals(URI.create(ProblemType.REQUISICAO_INVALIDA), problem.getType());
            assertEquals("O nome é obrigatório", problem.getDetail());
        }
    }

    @Nested
    @DisplayName("erros de protocolo")
    class DeProtocolo {

        /** Alvo só para dar ao BindingResult propriedades reais para rejeitar. */
        @SuppressWarnings("unused")
        static class CadastroFicticio {
            public String getName() {
                return null;
            }

            public String getEmail() {
                return null;
            }
        }

        @SuppressWarnings("unused")
        void endpointFicticio(CadastroFicticio corpo) {
            // Apenas empresta a assinatura para montar o MethodParameter dos testes.
        }

        private MethodParameter parametroDoEndpoint() throws NoSuchMethodException {
            return new MethodParameter(DeProtocolo.class
                    .getDeclaredMethod("endpointFicticio", CadastroFicticio.class), 0);
        }

        @Test
        @DisplayName("Bean Validation vira 400 agregando todos os campos inválidos")
        void validacao() throws Exception {
            BindingResult bindingResult =
                    new BeanPropertyBindingResult(new CadastroFicticio(), "userRegistrationRequest");
            bindingResult.rejectValue("name", "NotBlank", "O nome é obrigatório");
            bindingResult.rejectValue("email", "Email", "E-mail inválido");

            ProblemDetail problem = handler.handleValidation(
                    new MethodArgumentNotValidException(parametroDoEndpoint(), bindingResult));

            assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
            assertEquals(URI.create(ProblemType.REQUISICAO_INVALIDA), problem.getType());
            assertEquals("Um ou mais campos são inválidos", problem.getDetail());
            assertEquals(Map.of("name", "O nome é obrigatório", "email", "E-mail inválido"),
                    problem.getProperties().get("errors"));
        }

        /**
         * A mensagem original não é repassada de propósito: ela descreve a expressão
         * de autorização que barrou o acesso, detalhe interno da regra.
         */
        @Test
        @DisplayName("acesso negado pelo Spring Security vira 403 genérico")
        void acessoNegado() {
            ProblemDetail problem = handler.handleAccessDenied(
                    new AccessDeniedException("Access Denied for expression hasRole('ADMIN')"));

            assertEquals(HttpStatus.FORBIDDEN.value(), problem.getStatus());
            assertEquals("Acesso negado", problem.getTitle());
            assertEquals(URI.create(ProblemType.ACESSO_NEGADO), problem.getType());
            assertEquals("Você não tem permissão para acessar este recurso", problem.getDetail());
            assertFalse(problem.getDetail().contains("hasRole"));
        }

        @Test
        @DisplayName("id fora do formato UUID vira 400 nomeando o parâmetro")
        void tipoIncompativel() throws Exception {
            ProblemDetail problem = handler.handleTypeMismatch(
                    new MethodArgumentTypeMismatchException("nao-e-um-uuid", UUID.class, "id",
                            parametroDoEndpoint(), new IllegalArgumentException("formato inválido")));

            assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
            assertEquals(URI.create(ProblemType.REQUISICAO_INVALIDA), problem.getType());
            assertEquals("O parâmetro 'id' está em formato inválido", problem.getDetail());
        }

        @Test
        @DisplayName("JSON malformado vira 400 sem repassar a mensagem do parser")
        void corpoIlegivel() {
            ProblemDetail problem = handler.handleMessageNotReadable(
                    new HttpMessageNotReadableException(
                            "Unexpected character ('n' (code 110)): was expecting double-quote",
                            new MockHttpInputMessage(new byte[0])));

            assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
            assertEquals(URI.create(ProblemType.REQUISICAO_INVALIDA), problem.getType());
            assertEquals("O corpo da requisição está ausente ou malformado", problem.getDetail());
            assertFalse(problem.getDetail().contains("code 110"));
        }
    }

    @Nested
    @DisplayName("rede de segurança")
    class RedeDeSeguranca {

        @Test
        @DisplayName("erro não previsto vira 500 sem vazar detalhes internos")
        void erroNaoPrevisto() {
            ProblemDetail problem = handler.handleGeneric(new IllegalStateException(
                    "FATAL: password authentication failed for user postgres"));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
            assertEquals("Erro inesperado", problem.getTitle());
            assertEquals(URI.create(ProblemType.ERRO_INTERNO), problem.getType());
            assertEquals("Ocorreu um erro interno. Tente novamente mais tarde.", problem.getDetail());
            assertFalse(problem.getDetail().contains("postgres"));
        }
    }

    @Test
    @DisplayName("todo problema carrega um timestamp")
    void timestamp() {
        ProblemDetail problem = handler.handleNotFound(ResourceNotFoundException.user(UUID.randomUUID()));

        assertTrue(problem.getProperties().containsKey("timestamp"));
    }

    @Test
    @DisplayName("ProblemType expõe URIs estáveis e não é instanciável")
    void problemType() throws Exception {
        assertEquals("/problemas/recurso-nao-encontrado", ProblemType.RECURSO_NAO_ENCONTRADO);
        assertEquals("/problemas/conflito-de-dados", ProblemType.CONFLITO_DE_DADOS);
        assertEquals("/problemas/senha-invalida", ProblemType.SENHA_INVALIDA);
        assertEquals("/problemas/operacao-nao-permitida", ProblemType.OPERACAO_NAO_PERMITIDA);
        assertEquals("/problemas/acesso-negado", ProblemType.ACESSO_NEGADO);
        assertEquals("/problemas/requisicao-invalida", ProblemType.REQUISICAO_INVALIDA);
        assertEquals("/problemas/falha-autenticacao", ProblemType.FALHA_AUTENTICACAO);
        assertEquals("/problemas/nao-autenticado", ProblemType.NAO_AUTENTICADO);
        assertEquals("/problemas/token-invalido-ou-expirado", ProblemType.TOKEN_INVALIDO_OU_EXPIRADO);
        assertEquals("/problemas/erro-interno", ProblemType.ERRO_INTERNO);

        Constructor<ProblemType> constructor = ProblemType.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null));

        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}

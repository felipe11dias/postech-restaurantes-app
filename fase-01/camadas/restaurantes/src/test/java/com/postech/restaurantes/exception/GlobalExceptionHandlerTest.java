package com.postech.restaurantes.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler — erros do cliente e rede de segurança")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("corpo da requisição ilegível")
    class CorpoIlegivel {

        private HttpMessageNotReadableException erroDeParse(String mensagem) {
            return new HttpMessageNotReadableException(
                    mensagem, new MockHttpInputMessage(new byte[0]));
        }

        @Test
        @DisplayName("JSON malformado vira 400, e não 500")
        void jsonMalformado_deveRetornar400() {
            ProblemDetail problem = handler.handleMessageNotReadable(
                    erroDeParse("JSON parse error: Unexpected character ('n')"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getTitle()).isEqualTo("Requisição inválida");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.REQUISICAO_INVALIDA));
        }

        /**
         * A mensagem do Jackson descreve a posição do caractere e a estrutura
         * esperada — detalhe interno da desserialização, que não deve chegar a
         * quem chama a API.
         */
        @Test
        @DisplayName("não repassa a mensagem do parser ao cliente")
        void naoDeveVazarMensagemDoParser() {
            ProblemDetail problem = handler.handleMessageNotReadable(
                    erroDeParse("Unexpected character ('n' (code 110)): was expecting double-quote"));

            assertThat(problem.getDetail())
                    .isEqualTo("O corpo da requisição está ausente ou malformado")
                    .doesNotContain("code 110", "double-quote");
        }
    }

    @Nested
    @DisplayName("erro não previsto")
    class ErroNaoPrevisto {

        @Test
        @DisplayName("vira 500 com resposta genérica")
        void deveRetornar500Generico() {
            ProblemDetail problem = handler.handleGeneric(new IllegalStateException("conexão recusada"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(problem.getTitle()).isEqualTo("Erro inesperado");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.ERRO_INTERNO));
        }

        @Test
        @DisplayName("não vaza a mensagem da exceção original")
        void naoDeveVazarMensagemInterna() {
            ProblemDetail problem = handler.handleGeneric(
                    new IllegalStateException("FATAL: password authentication failed for user postgres"));

            assertThat(problem.getDetail())
                    .isEqualTo("Ocorreu um erro interno. Tente novamente mais tarde.")
                    .doesNotContain("password", "postgres");
        }
    }

    /**
     * Cada categoria de erro precisa chegar ao cliente com o seu próprio status e
     * o seu próprio "type" — é o que permite diferenciar os erros de forma
     * programática, em vez de depender do texto do título.
     */
    @Nested
    @DisplayName("categorias de erro")
    class Categorias {

        @Test
        @DisplayName("recurso não encontrado vira 404")
        void naoEncontrado_deveRetornar404() {
            ProblemDetail problem = handler.handleNotFound(
                    new ResourceNotFoundException("Usuário não encontrado: 42"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(problem.getTitle()).isEqualTo("Recurso não encontrado");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.RECURSO_NAO_ENCONTRADO));
            assertThat(problem.getDetail()).isEqualTo("Usuário não encontrado: 42");
        }

        @Test
        @DisplayName("valor único duplicado vira 409")
        void duplicado_deveRetornar409() {
            ProblemDetail problem = handler.handleDuplicate(
                    new DuplicateResourceException("Já existe um usuário com o e-mail joao@email.com"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(problem.getTitle()).isEqualTo("Conflito de dados");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.CONFLITO_DE_DADOS));
            assertThat(problem.getDetail()).contains("joao@email.com");
        }

        @Test
        @DisplayName("senha inválida vira 400")
        void senhaInvalida_deveRetornar400() {
            ProblemDetail problem = handler.handleInvalidPassword(
                    new InvalidPasswordException("A senha atual está incorreta"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getTitle()).isEqualTo("Senha inválida");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.SENHA_INVALIDA));
        }

        @Test
        @DisplayName("token de redefinição inválido ou expirado vira 400")
        void tokenInvalido_deveRetornar400() {
            ProblemDetail problem = handler.handleInvalidOrExpiredToken(
                    new InvalidOrExpiredTokenException("Token inválido ou expirado"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getTitle()).isEqualTo("Token inválido ou expirado");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.TOKEN_INVALIDO_OU_EXPIRADO));
        }

        @Test
        @DisplayName("operação vetada por regra de negócio vira 403")
        void operacaoVetada_deveRetornar403() {
            ProblemDetail problem = handler.handleForbiddenOperation(
                    new ForbiddenOperationException("Não é permitido se autocadastrar com o papel ROLE_ADMIN"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(problem.getTitle()).isEqualTo("Operação não permitida");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.OPERACAO_NAO_PERMITIDA));
            assertThat(problem.getDetail()).contains("ROLE_ADMIN");
        }

        /**
         * Aqui a mensagem original não é repassada de propósito: ela descreve a
         * expressão de autorização que barrou o acesso, detalhe interno da regra.
         */
        @Test
        @DisplayName("acesso negado pelo Spring Security vira 403 genérico")
        void acessoNegado_deveRetornar403Generico() {
            ProblemDetail problem = handler.handleAccessDenied(
                    new AccessDeniedException("Access Denied for expression hasRole('ADMIN')"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(problem.getTitle()).isEqualTo("Acesso negado");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.ACESSO_NEGADO));
            assertThat(problem.getDetail())
                    .isEqualTo("Você não tem permissão para acessar este recurso")
                    .doesNotContain("hasRole");
        }

        @Test
        @DisplayName("credenciais inválidas viram 401 sem dizer qual campo falhou")
        void credenciaisInvalidas_deveRetornar401() {
            ProblemDetail problem = handler.handleAuthentication(
                    new BadCredentialsException("Bad credentials"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problem.getTitle()).isEqualTo("Falha na autenticação");
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.FALHA_AUTENTICACAO));
            assertThat(problem.getDetail()).isEqualTo("Login ou senha inválidos");
        }

        @Test
        @DisplayName("falha de VO de valor (e-mail/CEP) vira 400 com a mensagem do domínio")
        void argumentoIlegal_deveRetornar400() {
            ProblemDetail problem = handler.handleIllegalArgument(
                    new IllegalArgumentException("CEP inválido (esperado 8 dígitos)"));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.REQUISICAO_INVALIDA));
            assertThat(problem.getDetail()).isEqualTo("CEP inválido (esperado 8 dígitos)");
        }
    }

    @Nested
    @DisplayName("erros de formato da requisição")
    class FormatoDaRequisicao {

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
            return new MethodParameter(
                    FormatoDaRequisicao.class.getDeclaredMethod("endpointFicticio", CadastroFicticio.class), 0);
        }

        @Test
        @DisplayName("Bean Validation vira 400 agregando todos os campos inválidos")
        void validacao_deveAgregarOsCampos() throws Exception {
            BindingResult bindingResult =
                    new BeanPropertyBindingResult(new CadastroFicticio(), "userRegistrationRequest");
            bindingResult.rejectValue("name", "NotBlank", "O nome é obrigatório");
            bindingResult.rejectValue("email", "Email", "E-mail inválido");

            ProblemDetail problem = handler.handleValidation(
                    new MethodArgumentNotValidException(parametroDoEndpoint(), bindingResult));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.REQUISICAO_INVALIDA));
            assertThat(problem.getDetail()).isEqualTo("Um ou mais campos são inválidos");
            assertThat(problem.getProperties()).extractingByKey("errors")
                    .isEqualTo(Map.of("name", "O nome é obrigatório", "email", "E-mail inválido"));
        }

        /**
         * Um {id} que não é UUID é erro de quem chama; sem este tratamento a falha
         * cairia no handler genérico e seria reportada como 500.
         */
        @Test
        @DisplayName("id fora do formato UUID vira 400 nomeando o parâmetro")
        void tipoIncompativel_deveRetornar400() throws Exception {
            ProblemDetail problem = handler.handleTypeMismatch(
                    new MethodArgumentTypeMismatchException("nao-e-um-uuid", UUID.class, "id",
                            parametroDoEndpoint(), new IllegalArgumentException("formato inválido")));

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getType()).isEqualTo(URI.create(ProblemType.REQUISICAO_INVALIDA));
            assertThat(problem.getDetail()).isEqualTo("O parâmetro 'id' está em formato inválido");
        }
    }

    @Test
    @DisplayName("todo problema carrega um timestamp")
    void todoProblemaDeveTerTimestamp() {
        ProblemDetail problem = handler.handleNotFound(
                new ResourceNotFoundException("Usuário não encontrado"));

        assertThat(problem.getProperties()).containsKey("timestamp");
    }
}

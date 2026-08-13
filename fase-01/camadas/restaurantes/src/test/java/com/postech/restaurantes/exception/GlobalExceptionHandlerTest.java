package com.postech.restaurantes.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import java.net.URI;

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

    @Test
    @DisplayName("todo problema carrega um timestamp")
    void todoProblemaDeveTerTimestamp() {
        ProblemDetail problem = handler.handleNotFound(
                new ResourceNotFoundException("Usuário não encontrado"));

        assertThat(problem.getProperties()).containsKey("timestamp");
    }
}

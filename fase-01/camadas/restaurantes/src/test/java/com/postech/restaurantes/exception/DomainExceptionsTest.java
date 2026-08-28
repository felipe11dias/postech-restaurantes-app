package com.postech.restaurantes.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As exceções de negócio são não checadas e carregam apenas a mensagem que o
 * GlobalExceptionHandler transforma em ProblemDetail. Estes testes fixam esse
 * contrato mínimo — mensagem preservada e tipo não checado.
 */
@DisplayName("Exceções de negócio — mensagem e tipo")
class DomainExceptionsTest {

    @Test
    @DisplayName("ResourceNotFoundException preserva a mensagem")
    void resourceNotFound_devePreservarMensagem() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Usuário não encontrado: 42");

        assertThat(exception).isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado: 42");
    }

    @Test
    @DisplayName("DuplicateResourceException preserva a mensagem")
    void duplicateResource_devePreservarMensagem() {
        DuplicateResourceException exception =
                new DuplicateResourceException("Já existe um usuário com o login joao");

        assertThat(exception).isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um usuário com o login joao");
    }

    @Test
    @DisplayName("InvalidPasswordException preserva a mensagem")
    void invalidPassword_devePreservarMensagem() {
        InvalidPasswordException exception = new InvalidPasswordException("A senha atual está incorreta");

        assertThat(exception).isInstanceOf(RuntimeException.class)
                .hasMessage("A senha atual está incorreta");
    }

    @Test
    @DisplayName("InvalidOrExpiredTokenException preserva a mensagem")
    void invalidOrExpiredToken_devePreservarMensagem() {
        InvalidOrExpiredTokenException exception =
                new InvalidOrExpiredTokenException("Token inválido ou expirado");

        assertThat(exception).isInstanceOf(RuntimeException.class)
                .hasMessage("Token inválido ou expirado");
    }

    @Test
    @DisplayName("ForbiddenOperationException preserva a mensagem")
    void forbiddenOperation_devePreservarMensagem() {
        ForbiddenOperationException exception =
                new ForbiddenOperationException("Não é permitido se autocadastrar com o papel ROLE_ADMIN");

        assertThat(exception).isInstanceOf(RuntimeException.class)
                .hasMessage("Não é permitido se autocadastrar com o papel ROLE_ADMIN");
    }

    @Test
    @DisplayName("ProblemType expõe URIs estáveis e não é instanciável")
    void problemType_deveExporUrisEstaveis() throws Exception {
        assertThat(ProblemType.RECURSO_NAO_ENCONTRADO).isEqualTo("/problemas/recurso-nao-encontrado");
        assertThat(ProblemType.CONFLITO_DE_DADOS).isEqualTo("/problemas/conflito-de-dados");
        assertThat(ProblemType.SENHA_INVALIDA).isEqualTo("/problemas/senha-invalida");
        assertThat(ProblemType.OPERACAO_NAO_PERMITIDA).isEqualTo("/problemas/operacao-nao-permitida");
        assertThat(ProblemType.ACESSO_NEGADO).isEqualTo("/problemas/acesso-negado");
        assertThat(ProblemType.REQUISICAO_INVALIDA).isEqualTo("/problemas/requisicao-invalida");
        assertThat(ProblemType.FALHA_AUTENTICACAO).isEqualTo("/problemas/falha-autenticacao");
        assertThat(ProblemType.NAO_AUTENTICADO).isEqualTo("/problemas/nao-autenticado");
        assertThat(ProblemType.TOKEN_INVALIDO_OU_EXPIRADO).isEqualTo("/problemas/token-invalido-ou-expirado");
        assertThat(ProblemType.ERRO_INTERNO).isEqualTo("/problemas/erro-interno");

        Constructor<ProblemType> constructor = ProblemType.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();

        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}

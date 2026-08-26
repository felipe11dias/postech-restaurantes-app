package com.postech.restaurantes.adapter.in.web.exception;

import com.postech.restaurantes.domain.exception.AuthenticationFailedException;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz as exceções do núcleo em respostas HTTP no padrão ProblemDetail (RFC 7807).
 *
 * <p>Esta classe é a materialização de uma fronteira. O domínio lança
 * {@code DuplicateResourceException} sem saber que isso é um 409; quem conhece
 * HTTP é o adapter de entrada, e é aqui — em um único ponto — que a tradução
 * acontece. Trocar REST por gRPC significaria escrever outro tradutor, sem tocar
 * em nenhuma exceção de negócio.</p>
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    // ----- exceções do núcleo -----

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ProblemType.RECURSO_NAO_ENCONTRADO,
                "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ProblemType.CONFLITO_DE_DADOS,
                "Conflito de dados", ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(InvalidPasswordException ex) {
        return build(HttpStatus.BAD_REQUEST, ProblemType.SENHA_INVALIDA,
                "Senha inválida", ex.getMessage());
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ProblemDetail handleInvalidOrExpiredToken(InvalidOrExpiredTokenException ex) {
        return build(HttpStatus.BAD_REQUEST, ProblemType.TOKEN_INVALIDO_OU_EXPIRADO,
                "Token inválido ou expirado", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbiddenOperation(ForbiddenOperationException ex) {
        return build(HttpStatus.FORBIDDEN, ProblemType.OPERACAO_NAO_PERMITIDA,
                "Operação não permitida", ex.getMessage());
    }

    /** Credenciais inválidas na validação de login — regra do caso de uso, não do framework. */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail handleAuthenticationFailed(AuthenticationFailedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ProblemType.FALHA_AUTENTICACAO,
                "Falha na autenticação", ex.getMessage());
    }

    /**
     * Invariantes de Value Object violadas (e-mail ou CEP malformados) e guardas de
     * argumento do domínio. Chegam como {@code IllegalArgumentException} porque é o
     * que o núcleo usa para "você me passou um valor que não faz sentido" — e isso
     * é, do lado do HTTP, um erro do cliente.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ProblemType.REQUISICAO_INVALIDA,
                "Requisição inválida", ex.getMessage());
    }

    // ----- erros de protocolo / borda -----

    /** Erros de Bean Validation nos DTOs — agrega todos os campos inválidos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, ProblemType.REQUISICAO_INVALIDA,
                "Requisição inválida", "Um ou mais campos são inválidos");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Acesso negado pelo Spring Security (ex.: recurso de outro usuário). */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ProblemType.ACESSO_NEGADO, "Acesso negado",
                "Você não tem permissão para acessar este recurso");
    }

    /**
     * Parâmetro de rota com formato incompatível — tipicamente um {id} que não é um
     * UUID válido. Sem este tratamento a falha cairia no handler genérico e um erro
     * do cliente seria reportado como 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, ProblemType.REQUISICAO_INVALIDA,
                "Requisição inválida",
                "O parâmetro '" + ex.getName() + "' está em formato inválido");
    }

    /**
     * Corpo ausente ou que o Jackson não consegue desserializar. A mensagem do
     * parser não é repassada: expõe detalhes internos da desserialização sem
     * ajudar quem chama a API.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Corpo de requisição ilegível", ex);
        return build(HttpStatus.BAD_REQUEST, ProblemType.REQUISICAO_INVALIDA,
                "Requisição inválida", "O corpo da requisição está ausente ou malformado");
    }

    /**
     * Rede de segurança para o que não foi previsto. A resposta é deliberadamente
     * genérica para não vazar detalhes internos — e é justamente por isso que a
     * exceção precisa ser registrada aqui: sem este log, um 500 não deixaria
     * rastro algum e a causa se perderia.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Erro não tratado processando a requisição", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ProblemType.ERRO_INTERNO,
                "Erro inesperado", "Ocorreu um erro interno. Tente novamente mais tarde.");
    }

    private ProblemDetail build(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}

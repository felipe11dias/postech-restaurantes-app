package com.postech.restaurantes.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratamento centralizado de erros. Toda resposta de erro segue o padrão
 * ProblemDetail (RFC 7807), nativo do Spring 6, com um timestamp adicional.
 * Cada categoria de erro recebe um "type" próprio (ProblemType), para que o
 * consumidor da API consiga diferenciar os erros de forma programática.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    /** Token de redefinição de senha inexistente, expirado ou já utilizado. */
    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ProblemDetail handleInvalidOrExpiredToken(InvalidOrExpiredTokenException ex) {
        return build(HttpStatus.BAD_REQUEST, ProblemType.TOKEN_INVALIDO_OU_EXPIRADO,
                "Token inválido ou expirado", ex.getMessage());
    }

    /** Operação vetada por regra de negócio (ex.: autocadastro com papel privilegiado). */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbiddenOperation(ForbiddenOperationException ex) {
        return build(HttpStatus.FORBIDDEN, ProblemType.OPERACAO_NAO_PERMITIDA,
                "Operação não permitida", ex.getMessage());
    }

    /** Acesso negado pelo Spring Security (ex.: recurso de outro usuário). */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ProblemType.ACESSO_NEGADO, "Acesso negado",
                "Você não tem permissão para acessar este recurso");
    }

    /** Erros de Bean Validation nos VOs de entrada — agrega todos os campos inválidos. */
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

    /**
     * Parâmetro de rota com formato incompatível — tipicamente um {id} que não é
     * um UUID válido. Sem este tratamento a falha cairia no handler genérico (500),
     * escondendo um erro que é do cliente.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, ProblemType.REQUISICAO_INVALIDA,
                "Requisição inválida",
                "O parâmetro '" + ex.getName() + "' está em formato inválido");
    }

    /**
     * Corpo da requisição ausente ou que o Jackson não consegue desserializar
     * (JSON malformado, tipo incompatível). Assim como o descasamento de tipo
     * acima, é um erro do cliente que sem tratamento cairia no handler genérico
     * e seria reportado como 500.
     *
     * A mensagem do parser não é repassada: ela expõe detalhes internos da
     * desserialização sem ajudar quem chama a API.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Corpo de requisição ilegível", ex);
        return build(HttpStatus.BAD_REQUEST, ProblemType.REQUISICAO_INVALIDA,
                "Requisição inválida",
                "O corpo da requisição está ausente ou malformado");
    }

    /** Falhas de validação dos VOs de valor (ex.: e-mail/CEP malformado). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ProblemType.REQUISICAO_INVALIDA,
                "Requisição inválida", ex.getMessage());
    }

    /** Credenciais inválidas na validação de login. */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ProblemType.FALHA_AUTENTICACAO,
                "Falha na autenticação", "Login ou senha inválidos");
    }

    /**
     * Rede de segurança para o que não foi previsto. A resposta é deliberadamente
     * genérica, para não vazar detalhes internos ao cliente — e é justamente por
     * isso que a exceção precisa ser registrada aqui: sem este log, um 500 não
     * deixa rastro algum e a causa se perde.
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

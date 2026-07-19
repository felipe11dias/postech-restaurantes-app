package com.postech.restaurantes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
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

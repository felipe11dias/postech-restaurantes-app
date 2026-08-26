package com.postech.restaurantes.adapter.in.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postech.restaurantes.adapter.in.web.exception.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Responde requisições não autenticadas a endpoints protegidos com um
 * ProblemDetail (RFC 7807).
 *
 * <p>Existe porque essas requisições são barradas pela cadeia de filtros, antes de
 * chegar a qualquer controller — e, portanto, fora do alcance do
 * {@code @RestControllerAdvice}. Sem este ponto de entrada, um 401 sairia no
 * formato padrão do container, quebrando a consistência do corpo de erro que o
 * resto da API mantém.</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Autenticação necessária para acessar este recurso");
        problem.setType(URI.create(ProblemType.NAO_AUTENTICADO));
        problem.setTitle("Não autenticado");
        problem.setProperty("timestamp", Instant.now());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}

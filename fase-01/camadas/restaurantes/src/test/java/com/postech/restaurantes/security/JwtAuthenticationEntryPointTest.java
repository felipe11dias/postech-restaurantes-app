package com.postech.restaurantes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.postech.restaurantes.exception.ProblemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requisição não autenticada a endpoint protegido precisa responder no mesmo
 * padrão de erro do resto da API (ProblemDetail, RFC 7807) — e não no HTML
 * padrão do container.
 */
@DisplayName("JwtAuthenticationEntryPoint — resposta a requisição não autenticada")
class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    @DisplayName("responde 401 com ProblemDetail em application/problem+json")
    void commence_deveResponder401ComProblemDetail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ObjectNode corpo = (ObjectNode) objectMapper.readTree(response.getContentAsByteArray());
        assertThat(corpo.get("status").asInt()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(corpo.get("type").asText()).isEqualTo(ProblemType.NAO_AUTENTICADO);
        assertThat(corpo.get("title").asText()).isEqualTo("Não autenticado");
        assertThat(corpo.get("detail").asText())
                .isEqualTo("Autenticação necessária para acessar este recurso");
        assertThat(corpo.has("timestamp")).isTrue();
    }
}

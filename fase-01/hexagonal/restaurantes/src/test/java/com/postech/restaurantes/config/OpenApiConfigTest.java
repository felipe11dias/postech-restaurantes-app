package com.postech.restaurantes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OpenApiConfig — documentação do adapter web")
class OpenApiConfigTest {

    private final OpenAPI openAPI = new OpenApiConfig().restaurantesOpenAPI();

    @Test
    @DisplayName("descreve título, descrição e versão da API")
    void descreveAApi() {
        assertEquals("API de Gestão de Restaurantes (Arquitetura Hexagonal)",
                openAPI.getInfo().getTitle());
        assertEquals("v1", openAPI.getInfo().getVersion());
        assertTrue(openAPI.getInfo().getDescription().contains("Ports & Adapters"));
    }

    /** É o esquema declarado aqui que habilita o botão "Authorize" no Swagger UI. */
    @Test
    @DisplayName("registra o esquema de segurança Bearer JWT")
    void registraOEsquemaBearer() {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

        assertNotNull(scheme);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }

    @Test
    @DisplayName("aplica o esquema Bearer como requisito padrão da API")
    void aplicaOEsquemaComoRequisito() {
        assertEquals(1, openAPI.getSecurity().size());
        assertTrue(openAPI.getSecurity().get(0).containsKey("bearerAuth"));
    }
}

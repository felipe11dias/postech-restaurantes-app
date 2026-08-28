package com.postech.restaurantes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig — documentação da API")
class OpenApiConfigTest {

    private final OpenAPI openAPI = new OpenApiConfig().restaurantesOpenAPI();

    @Test
    @DisplayName("descreve título, descrição e versão da API")
    void deveDescreverAApi() {
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("API de Gestão de Restaurantes");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getInfo().getDescription()).contains("Tech Challenge Fase 1");
    }

    /** É o esquema declarado aqui que habilita o botão "Authorize" no Swagger UI. */
    @Test
    @DisplayName("registra o esquema de segurança Bearer JWT")
    void deveRegistrarOEsquemaBearer() {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

        assertThat(scheme).isNotNull();
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("aplica o esquema Bearer como requisito padrão da API")
    void deveAplicarOEsquemaComoRequisito() {
        assertThat(openAPI.getSecurity()).singleElement()
                .satisfies(requisito -> assertThat(requisito).containsKey("bearerAuth"));
    }
}

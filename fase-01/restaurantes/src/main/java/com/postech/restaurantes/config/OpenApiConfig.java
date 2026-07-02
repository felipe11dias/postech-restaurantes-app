package com.postech.restaurantes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação OpenAPI (Swagger). Registra o esquema de
 * segurança Bearer JWT, habilitando o botão "Authorize" no Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI restaurantesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestão de Restaurantes")
                        .description("Backend do Tech Challenge Fase 1 — gestão de usuários "
                                + "(donos de restaurante e clientes), autenticação JWT.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}

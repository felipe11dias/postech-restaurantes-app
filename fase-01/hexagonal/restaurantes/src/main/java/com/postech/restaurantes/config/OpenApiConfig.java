package com.postech.restaurantes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI (Swagger), gerada a partir do adapter de entrada web.
 *
 * <p>Registra o esquema de segurança Bearer JWT, o que habilita o botão
 * <em>Authorize</em> no Swagger UI. Por descrever o adapter, e não o núcleo, a
 * documentação não contamina o hexágono: um segundo adapter de entrada teria a
 * sua própria — ou nenhuma.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI restaurantesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestão de Restaurantes (Arquitetura Hexagonal)")
                        .description("Backend do Tech Challenge Fase 1 — gestão de usuários "
                                + "(donos de restaurante e clientes), autenticação JWT. "
                                + "Variante em Ports & Adapters.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}

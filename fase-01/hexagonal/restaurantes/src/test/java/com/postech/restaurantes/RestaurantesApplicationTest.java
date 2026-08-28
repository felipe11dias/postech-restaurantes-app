package com.postech.restaurantes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

/**
 * Adapter de entrada do Spring Boot: só delega ao SpringApplication. Substituir
 * essa chamada permite verificar a delegação sem subir o contexto — que exigiria
 * banco disponível, algo que não cabe em um teste de unidade.
 */
@DisplayName("RestaurantesApplication — ponto de entrada")
class RestaurantesApplicationTest {

    @Test
    @DisplayName("main delega a inicialização ao SpringApplication")
    void mainDelega() {
        String[] argumentos = {"--server.port=0"};

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            RestaurantesApplication.main(argumentos);

            spring.verify(() -> SpringApplication.run(RestaurantesApplication.class, argumentos));
        }
    }

    @Test
    @DisplayName("a classe de entrada é instanciável pelo Spring")
    void instanciavel() {
        assertNotNull(new RestaurantesApplication());
    }
}

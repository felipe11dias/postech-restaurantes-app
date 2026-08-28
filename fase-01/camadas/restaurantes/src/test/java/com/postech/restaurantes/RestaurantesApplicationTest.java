package com.postech.restaurantes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * A classe de entrada só delega ao SpringApplication. Substituir essa chamada
 * permite verificar a delegação sem subir o contexto inteiro — o que exigiria
 * banco e SMTP disponíveis para um teste que não é de integração.
 */
@DisplayName("RestaurantesApplication — ponto de entrada")
class RestaurantesApplicationTest {

    @Test
    @DisplayName("main delega a inicialização ao SpringApplication")
    void main_deveDelegarAoSpringApplication() {
        String[] argumentos = {"--server.port=0"};

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            RestaurantesApplication.main(argumentos);

            spring.verify(() -> SpringApplication.run(RestaurantesApplication.class, argumentos));
        }
    }

    @Test
    @DisplayName("a classe de entrada é instanciável pelo Spring")
    void deveSerInstanciavel() {
        assertThat(new RestaurantesApplication()).isNotNull();
    }
}

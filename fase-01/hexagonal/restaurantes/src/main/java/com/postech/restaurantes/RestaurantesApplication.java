package com.postech.restaurantes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação (adapter de entrada do Spring Boot).
 *
 * <p>O núcleo hexagonal (pacotes {@code domain} e {@code application}) permanece agnóstico
 * a framework; é a injeção de dependência do Spring que, em tempo de execução, conecta cada
 * porta à sua implementação nos adapters ({@code adapter.in} / {@code adapter.out}).</p>
 */
@SpringBootApplication
public class RestaurantesApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestaurantesApplication.class, args);
    }
}

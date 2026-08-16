package com.postech.restaurantes.config;

import com.postech.restaurantes.adapter.in.web.security.JwtAuthenticationEntryPoint;
import com.postech.restaurantes.adapter.in.web.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração do Spring Security.
 *
 * <p>Note o que <em>não</em> está aqui: nenhum {@code AuthenticationManager} e
 * nenhum {@code UserDetailsService}. Validar credenciais é responsabilidade do
 * {@code AuthenticateUseCase}, dentro do hexágono; e como o token carrega os
 * papéis, o filtro monta a autenticação sem precisar recarregar o usuário do
 * banco. O Spring Security fica com o que é de fato dele: decidir quais rotas
 * exigem autenticação e como responder quando ela falta.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // A API é stateless e autenticada por token no cabeçalho; sem cookie de
                // sessão não há o vetor que o CSRF protege.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Autenticação e autocadastro são públicos
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        // Documentação e health checks
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Demais endpoints exigem token válido
                        .anyRequest().authenticated())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * O algoritmo concreto de hash. É consumido pelo {@code BCryptPasswordAdapter},
     * que o apresenta ao núcleo como um {@code PasswordEncoderPort} — o núcleo
     * nunca vê este tipo.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

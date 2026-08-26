package com.postech.restaurantes.application.port.out;

/**
 * Port de saída: codificação e conferência de senhas.
 *
 * <p>O núcleo declara esta abstração em vez de importar o {@code PasswordEncoder}
 * do Spring Security. É a diferença entre "a aplicação precisa transformar senha
 * em hash" (regra) e "a aplicação usa BCrypt do Spring" (detalhe). Trocar para
 * Argon2 é escrever outro adapter; nenhum caso de uso muda — Aberto/Fechado na
 * prática.</p>
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}

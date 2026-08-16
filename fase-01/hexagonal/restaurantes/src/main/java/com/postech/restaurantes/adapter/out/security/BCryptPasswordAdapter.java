package com.postech.restaurantes.adapter.out.security;

import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter de saída que realiza o {@link PasswordEncoderPort} com BCrypt.
 *
 * <p>É uma casca fina sobre o {@code PasswordEncoder} do Spring Security, e é
 * exatamente esse o ponto: a palavra "BCrypt" aparece aqui e no bean de
 * configuração, em lugar nenhum mais. Migrar para Argon2 é trocar esta classe.</p>
 */
@Component
public class BCryptPasswordAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}

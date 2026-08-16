package com.postech.restaurantes.adapter.out.security;

import com.postech.restaurantes.application.port.out.ResetTokenGeneratorPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Adapter de saída que realiza o {@link ResetTokenGeneratorPort}: tokens opacos
 * de 256 bits de entropia, com hash SHA-256 para persistência.
 *
 * <p>O token é guardado apenas como hash, pelo mesmo princípio da senha: quem
 * conseguir ler a tabela não consegue redefinir senhas com o que encontrar lá.
 * O algoritmo é independente do JWT de propósito — são segredos com propósitos e
 * ciclos de vida distintos.</p>
 */
@Component
public class SecureRandomResetTokenAdapter implements ResetTokenGeneratorPort {

    private static final int TOKEN_BYTES = 32;
    private static final String ALGORITHM = "SHA-256";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        try {
            byte[] hashed = MessageDigest.getInstance(ALGORITHM)
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash " + ALGORITHM + " indisponível", e);
        }
    }
}

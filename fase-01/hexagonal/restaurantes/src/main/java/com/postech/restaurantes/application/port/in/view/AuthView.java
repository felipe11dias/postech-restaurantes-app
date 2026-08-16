package com.postech.restaurantes.application.port.in.view;

/**
 * Resultado de uma autenticação bem-sucedida.
 *
 * <p>O campo {@code type} diz "Bearer" porque é assim que o token deve ser
 * apresentado, não porque o núcleo saiba o que é JWT. Trocar o mecanismo de token
 * (PASETO, tokens opacos) não muda esta view — muda o adapter que a preenche.</p>
 */
public record AuthView(String token, String type, long expiresIn) {

    public static AuthView bearer(String token, long expiresIn) {
        return new AuthView(token, "Bearer", expiresIn);
    }
}

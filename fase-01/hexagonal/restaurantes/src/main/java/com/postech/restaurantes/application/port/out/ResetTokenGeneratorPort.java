package com.postech.restaurantes.application.port.out;

/**
 * Port de saída: geração e hash dos tokens opacos de redefinição de senha.
 *
 * <p>Separado de {@link TokenProviderPort} porque resolve outro problema: o token
 * de acesso identifica uma sessão autenticada e é lido de volta; o token de reset
 * é um segredo de uso único, guardado apenas como hash e nunca interpretado. Unir
 * os dois em uma interface só faria cada implementação carregar um método que não
 * faz sentido para ela.</p>
 */
public interface ResetTokenGeneratorPort {

    /** Gera um token aleatório em claro, para ser enviado ao usuário. */
    String generateToken();

    /** Calcula o hash do token — é o hash, nunca o token, que vai para o banco. */
    String hash(String rawToken);
}

package com.postech.restaurantes.application.port.out;

import com.postech.restaurantes.domain.model.User;

/**
 * Port de saída: emissão de tokens de acesso.
 *
 * <p>Repare no que a interface <em>não</em> diz: não menciona JWT, assinatura,
 * claims ou HMAC. O núcleo pede "um token para este usuário, e me diga quanto
 * tempo ele dura"; que isso vire um JWT assinado com HMAC-SHA256 é decisão do
 * adapter de segurança.</p>
 */
public interface TokenProviderPort {

    String generateToken(User user);

    /** Tempo de vida do token emitido, em milissegundos. */
    long expirationInMillis();
}

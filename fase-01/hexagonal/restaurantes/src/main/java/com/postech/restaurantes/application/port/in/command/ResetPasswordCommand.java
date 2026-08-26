package com.postech.restaurantes.application.port.in.command;

/**
 * Redefinição de senha a partir de um token recebido por e-mail — o caminho de
 * quem esqueceu a senha e, por isso, não tem como informar a atual.
 *
 * @param rawToken token em claro, como veio no e-mail; o adapter de segurança
 *                 calcula o hash para comparar com o que está persistido
 */
public record ResetPasswordCommand(
        String rawToken,
        String newPassword,
        String confirmPassword
) {
}

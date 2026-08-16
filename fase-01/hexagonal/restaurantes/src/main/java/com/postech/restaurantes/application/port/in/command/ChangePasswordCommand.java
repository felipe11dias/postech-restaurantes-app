package com.postech.restaurantes.application.port.in.command;

import java.util.UUID;

/**
 * Comando de troca de senha por um usuário que sabe a senha atual.
 * Distinto de {@link ResetPasswordCommand}, que atende quem esqueceu a senha.
 */
public record ChangePasswordCommand(
        UUID userId,
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}

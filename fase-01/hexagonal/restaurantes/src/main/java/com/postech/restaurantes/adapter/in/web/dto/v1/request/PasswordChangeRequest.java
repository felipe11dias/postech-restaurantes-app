package com.postech.restaurantes.adapter.in.web.dto.v1.request;

import com.postech.restaurantes.application.port.in.command.ChangePasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de entrada para troca de senha (v1), usado no endpoint exclusivo de senha.
 * A conferência entre {@code newPassword} e {@code confirmPassword} é do caso de uso.
 */
public record PasswordChangeRequest(

        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, message = "A nova senha deve ter ao menos 8 caracteres")
        String newPassword,

        @NotBlank(message = "A confirmação de senha é obrigatória")
        String confirmPassword
) {

    public ChangePasswordCommand toCommand(UUID userId) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword, confirmPassword);
    }
}

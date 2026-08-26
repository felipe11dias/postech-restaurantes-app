package com.postech.restaurantes.adapter.in.web.dto.v1.request;

import com.postech.restaurantes.application.port.in.command.ResetPasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para redefinir a senha a partir de um token de recuperação (v1). */
public record ResetPasswordRequest(

        @NotBlank(message = "O token é obrigatório")
        String token,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, message = "A nova senha deve ter ao menos 8 caracteres")
        String newPassword,

        @NotBlank(message = "A confirmação de senha é obrigatória")
        String confirmPassword
) {

    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(token, newPassword, confirmPassword);
    }
}

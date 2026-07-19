package com.postech.restaurantes.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * VO de entrada para redefinir a senha a partir de um token de recuperação
 * (v1). A conferência entre newPassword e confirmPassword é feita no Service.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "O token é obrigatório")
        String token,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, message = "A nova senha deve ter ao menos 8 caracteres")
        String newPassword,

        @NotBlank(message = "A confirmação de senha é obrigatória")
        String confirmPassword
) {
}

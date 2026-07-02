package com.postech.restaurantes.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * VO de entrada para troca de senha (v1), usado no endpoint exclusivo de senha.
 * A conferência entre newPassword e confirmPassword é feita no Service.
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
}

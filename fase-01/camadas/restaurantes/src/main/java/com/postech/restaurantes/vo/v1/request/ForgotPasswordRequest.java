package com.postech.restaurantes.vo.v1.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * VO de entrada para solicitar a recuperação de senha (v1).
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email
) {
}

package com.postech.restaurantes.vo.v1.request;

import jakarta.validation.constraints.NotBlank;

/**
 * VO de entrada para validação de login (v1).
 */
public record LoginRequest(

        @NotBlank(message = "O login é obrigatório")
        String login,

        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}

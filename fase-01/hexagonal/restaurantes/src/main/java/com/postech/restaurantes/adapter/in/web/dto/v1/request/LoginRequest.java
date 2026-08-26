package com.postech.restaurantes.adapter.in.web.dto.v1.request;

import com.postech.restaurantes.application.port.in.command.AuthenticateCommand;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada para validação de login (v1). */
public record LoginRequest(

        @NotBlank(message = "O login é obrigatório")
        String login,

        @NotBlank(message = "A senha é obrigatória")
        String password
) {

    public AuthenticateCommand toCommand() {
        return new AuthenticateCommand(login, password);
    }
}

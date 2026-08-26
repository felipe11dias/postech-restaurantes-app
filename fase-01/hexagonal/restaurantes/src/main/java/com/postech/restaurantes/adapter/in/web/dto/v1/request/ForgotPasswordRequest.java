package com.postech.restaurantes.adapter.in.web.dto.v1.request;

import com.postech.restaurantes.application.port.in.command.RequestPasswordResetCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada para solicitar a recuperação de senha (v1). */
public record ForgotPasswordRequest(

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email
) {

    public RequestPasswordResetCommand toCommand() {
        return new RequestPasswordResetCommand(email);
    }
}

package com.postech.restaurantes.adapter.in.web.dto.v1.request;

import com.postech.restaurantes.application.port.in.command.UpdateUserCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para atualização dos dados do usuário (v1).
 * Não inclui senha — a troca de senha tem endpoint, DTO e caso de uso próprios.
 */
public record UserUpdateRequest(

        @NotBlank(message = "O nome é obrigatório")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "O login é obrigatório")
        String login,

        @Valid
        List<AddressRequest> addresses
) {

    /** O id vem do path, não do corpo: o recurso alterado é o da URL. */
    public UpdateUserCommand toCommand(UUID userId) {
        return new UpdateUserCommand(userId, name, email, login,
                UserRegistrationRequest.toAddressCommands(addresses));
    }
}

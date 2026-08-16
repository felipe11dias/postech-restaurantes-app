package com.postech.restaurantes.adapter.in.web.dto.v1.request;

import com.postech.restaurantes.application.port.in.command.AddressCommand;
import com.postech.restaurantes.application.port.in.command.RegisterUserCommand;
import com.postech.restaurantes.domain.model.RoleName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/**
 * DTO de entrada para cadastro de usuário (v1).
 *
 * <p>O método {@code toCommand} é a fronteira: daqui para dentro, ninguém mais
 * conhece este record. É o que permite versionar o contrato HTTP (um
 * {@code UserRegistrationRequest} v2 com campos diferentes) sem tocar no caso de
 * uso — basta que a nova versão saiba montar o mesmo command.</p>
 */
public record UserRegistrationRequest(

        @NotBlank(message = "O nome é obrigatório")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "O login é obrigatório")
        String login,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres")
        String password,

        @NotEmpty(message = "Informe ao menos um papel")
        Set<RoleName> roles,

        @Valid
        List<AddressRequest> addresses
) {

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(name, email, login, password, roles, toAddressCommands(addresses));
    }

    static List<AddressCommand> toAddressCommands(List<AddressRequest> addresses) {
        return addresses == null ? List.of() : addresses.stream().map(AddressRequest::toCommand).toList();
    }
}

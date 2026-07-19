package com.postech.restaurantes.vo.v1.request;

import com.postech.restaurantes.enums.RoleName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/**
 * VO de entrada para cadastro de usuário (v1).
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
}

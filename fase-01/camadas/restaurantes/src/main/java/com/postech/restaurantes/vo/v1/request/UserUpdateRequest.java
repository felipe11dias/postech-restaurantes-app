package com.postech.restaurantes.vo.v1.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * VO de entrada para atualização das demais informações do usuário (v1).
 * Não inclui senha — a troca de senha tem endpoint e VO próprios.
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
}

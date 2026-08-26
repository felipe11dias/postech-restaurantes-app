package com.postech.restaurantes.adapter.in.web.dto.v1.request;

import com.postech.restaurantes.application.port.in.command.AddressCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para endereço (v1).
 *
 * <p>A Bean Validation aqui é uma primeira barreira, de formato — ela devolve 400
 * com a lista de campos inválidos antes de o caso de uso ser chamado. Ela não
 * substitui a invariante do {@code ZipCode} no domínio: esta valida o que chegou
 * por HTTP; aquela garante que nenhum CEP inválido exista em um objeto de domínio,
 * venha ele de onde vier.</p>
 */
public record AddressRequest(

        @NotBlank(message = "A rua é obrigatória")
        String street,

        String number,

        String complement,

        String neighborhood,

        @NotBlank(message = "A cidade é obrigatória")
        String city,

        @NotBlank(message = "O estado é obrigatório")
        @Size(min = 2, max = 2, message = "O estado deve ser a UF com 2 letras")
        String state,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido (use 00000-000 ou 00000000)")
        String zipCode
) {

    public AddressCommand toCommand() {
        return new AddressCommand(street, number, complement, neighborhood, city, state, zipCode);
    }
}

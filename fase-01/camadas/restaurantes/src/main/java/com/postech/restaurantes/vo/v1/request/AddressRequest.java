package com.postech.restaurantes.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * VO de entrada para endereço (v1). Validação de formato via Bean Validation.
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
}

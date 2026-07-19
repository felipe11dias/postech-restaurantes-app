package com.postech.restaurantes.vo.v1.response;

/**
 * VO de saída para endereço (v1).
 */
public record AddressResponse(
        Long id,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {
}

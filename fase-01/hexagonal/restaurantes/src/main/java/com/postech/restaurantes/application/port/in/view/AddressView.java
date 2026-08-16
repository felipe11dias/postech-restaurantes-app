package com.postech.restaurantes.application.port.in.view;

import com.postech.restaurantes.domain.model.Address;

import java.util.UUID;

/** Projeção de saída de um endereço. O CEP sai já formatado (00000-000). */
public record AddressView(
        UUID id,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {

    public static AddressView from(Address address) {
        return new AddressView(
                address.getId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getZipCode().formatted());
    }
}

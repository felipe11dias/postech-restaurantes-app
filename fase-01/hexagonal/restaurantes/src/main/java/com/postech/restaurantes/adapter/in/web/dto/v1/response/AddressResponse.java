package com.postech.restaurantes.adapter.in.web.dto.v1.response;

import com.postech.restaurantes.application.port.in.view.AddressView;

import java.util.UUID;

/** DTO de saída para endereço (v1). */
public record AddressResponse(
        UUID id,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {

    public static AddressResponse from(AddressView view) {
        return new AddressResponse(view.id(), view.street(), view.number(), view.complement(),
                view.neighborhood(), view.city(), view.state(), view.zipCode());
    }
}

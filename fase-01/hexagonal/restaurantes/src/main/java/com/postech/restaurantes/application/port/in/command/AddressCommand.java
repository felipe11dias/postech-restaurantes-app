package com.postech.restaurantes.application.port.in.command;

/**
 * Endereço informado na entrada de um caso de uso.
 *
 * <p>É texto cru, ainda não validado: quem transforma {@code zipCode} em um
 * {@code ZipCode} válido é o serviço de aplicação, ao montar o domínio. Assim a
 * invariante do CEP é cobrada uma única vez, no lugar onde o objeto de domínio
 * nasce — e não replicada em cada adapter que porventura chame o caso de uso.</p>
 */
public record AddressCommand(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {
}

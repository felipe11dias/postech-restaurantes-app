package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.command.AddressCommand;
import com.postech.restaurantes.domain.model.Address;
import com.postech.restaurantes.domain.model.shared.ZipCode;

import java.util.List;

/**
 * Converte os endereços de um command em objetos de domínio.
 *
 * <p>Está em um único ponto porque cadastro e atualização precisam exatamente da
 * mesma conversão — e é aqui que o CEP cru vira um {@link ZipCode} validado e
 * normalizado, garantindo que a invariante seja cobrada nos dois caminhos.</p>
 */
final class AddressFactory {

    private AddressFactory() {
    }

    static List<Address> toDomain(List<AddressCommand> commands) {
        if (commands == null) {
            return List.of();
        }
        return commands.stream().map(AddressFactory::toDomain).toList();
    }

    private static Address toDomain(AddressCommand command) {
        return Address.newAddress(
                command.street(),
                command.number(),
                command.complement(),
                command.neighborhood(),
                command.city(),
                command.state(),
                new ZipCode(command.zipCode()));
    }
}

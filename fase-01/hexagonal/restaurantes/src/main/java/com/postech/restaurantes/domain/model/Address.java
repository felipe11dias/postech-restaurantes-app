package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.model.shared.ZipCode;
import com.postech.restaurantes.domain.util.ObjectUtils;

import java.util.UUID;

/**
 * Endereço de um usuário. Um usuário pode ter vários endereços (1:N), por isso o
 * endereço foi modelado como tabela própria (normalização), e não como objeto embutido.
 *
 * <p>Diferente da variante em camadas, aqui o endereço <strong>não</strong> guarda o
 * id do usuário. Ele vive dentro do agregado {@link User}, e quem conhece o vínculo
 * com a coluna {@code addresses.user_id} é o adapter de persistência. Um id de chave
 * estrangeira é vocabulário de banco, não de negócio — mantê-lo fora do domínio é
 * exatamente o tipo de vazamento de infraestrutura que o hexágono se propõe a evitar.</p>
 */
public class Address {

    private UUID id;
    private final String street;
    private final String number;
    private final String complement;
    private final String neighborhood;
    private final String city;
    private final String state;
    private final ZipCode zipCode;

    private Address(UUID id, String street, String number, String complement, String neighborhood,
                    String city, String state, ZipCode zipCode) {
        this.id = id;
        this.street = ObjectUtils.requireNonBlank(street, "A rua é obrigatória");
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = ObjectUtils.requireNonBlank(city, "A cidade é obrigatória");
        this.state = ObjectUtils.requireNonBlank(state, "O estado é obrigatório");
        this.zipCode = ObjectUtils.requireNonNull(zipCode, "O CEP é obrigatório");
    }

    /** Cria um endereço novo, ainda sem identidade. */
    public static Address newAddress(String street, String number, String complement,
                                     String neighborhood, String city, String state, ZipCode zipCode) {
        return new Address(null, street, number, complement, neighborhood, city, state, zipCode);
    }

    /** Reconstrói um endereço já persistido. Usado pelo adapter de persistência. */
    public static Address restore(UUID id, String street, String number, String complement,
                                  String neighborhood, String city, String state, ZipCode zipCode) {
        return new Address(id, street, number, complement, neighborhood, city, state, zipCode);
    }

    public UUID getId() {
        return id;
    }

    /** Atribuído uma única vez, pelo adapter de persistência, após o INSERT. */
    public void assignId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("O id do endereço já foi atribuído");
        }
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public String getComplement() {
        return complement;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public ZipCode getZipCode() {
        return zipCode;
    }
}

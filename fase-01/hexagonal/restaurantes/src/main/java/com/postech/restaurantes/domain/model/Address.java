package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.model.shared.ZipCode;

/**
 * Endereço de um usuário. Um usuário pode ter vários endereços (1:N),
 * por isso o endereço foi modelado como tabela própria (normalização),
 * e não como objeto embutido.
 */
public class Address {

    private Long id;

    private String street;

    private String number;

    private String complement;

    private String neighborhood;

    private String city;

    private String state;

    private ZipCode zipCode;

    /**
     * Decisão de modelagem: o vínculo com o usuário é mantido bidirecional, e a
     * sincronização dos dois lados é feita SEMPRE pelo {@link User} (addAddress/
     * removeAddress), pois é o usuário — raiz do agregado — quem usa e gerencia
     * os endereços.
     */
    private User user;

    public Address(Long id, String street, String number, String complement, String neighborhood, String city, String state, ZipCode zipCode, User user) {
        this.id = id;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.user = user;
    }

    public Long getId() {

        return id;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static Address newAddress(String street, String number, String complement, String neighborhood, String city, String state, ZipCode zipCode) {
        return new Address(null, street, number, complement, neighborhood, city, state, zipCode, null);
    }

}

package com.postech.restaurantes.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Endereço de um usuário. Um usuário pode ter vários endereços (1:N),
 * por isso o endereço foi modelado como tabela própria (normalização),
 * e não como objeto embutido.
 *
 * O vínculo com o usuário é o id (userId), não a entidade inteira: sem ORM não
 * há carregamento preguiçoso, e guardar o objeto User completo aqui obrigaria a
 * carregá-lo (com papéis e demais endereços) toda vez que um endereço fosse lido.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends Auditable {

    private UUID id;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;
    private String zipCode;
    private UUID userId;
}

package com.postech.restaurantes.mapper;

import com.postech.restaurantes.entity.Address;
import com.postech.restaurantes.vo.v1.request.AddressRequest;
import com.postech.restaurantes.vo.v1.response.AddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeia Address entre o VO de entrada/saída e a entidade.
 * Gerado em tempo de compilação pelo MapStruct.
 */
@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Address toEntity(AddressRequest request);

    AddressResponse toResponse(Address address);
}

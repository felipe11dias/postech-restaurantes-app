package com.postech.restaurantes.mapper;

import com.postech.restaurantes.entity.Address;
import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.vo.v1.response.RoleResponse;
import com.postech.restaurantes.vo.v1.response.UserResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapeia User entre VO e entidade. Campos resolvidos no Service ficam como
 * `ignore` aqui: a senha (que recebe hash) e os papéis (entidades Role buscadas
 * no banco). Gerado em tempo de compilação pelo MapStruct.
 */
@Mapper(componentModel = "spring", uses = AddressMapper.class)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toEntity(UserRegistrationRequest request);

    UserResponse toResponse(User user);

    @Mapping(target = "name", expression = "java(role.getName() != null ? role.getName().name() : null)")
    RoleResponse toRoleResponse(Role role);

    /**
     * Mantém a consistência do relacionamento bidirecional: cada endereço
     * recém-mapeado aponta de volta para o usuário.
     */
    @AfterMapping
    default void linkAddresses(@MappingTarget User user) {
        if (user.getAddresses() != null) {
            for (Address address : user.getAddresses()) {
                address.setUser(user);
            }
        }
    }
}

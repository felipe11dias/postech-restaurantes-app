package com.postech.restaurantes.adapter.in.web.dto.v1.response;

import com.postech.restaurantes.application.port.in.view.UserView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO de saída para usuário (v1). Nunca expõe a senha — e nem teria como: a
 * {@code UserView} de onde ele é montado também não a carrega.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String login,
        Set<RoleResponse> roles,
        List<AddressResponse> addresses,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {

    public static UserResponse from(UserView view) {
        return new UserResponse(
                view.id(),
                view.name(),
                view.email(),
                view.login(),
                view.roles().stream()
                        .map(RoleResponse::from)
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new)),
                view.addresses().stream().map(AddressResponse::from).toList(),
                view.createdAt(),
                view.lastUpdatedAt());
    }
}

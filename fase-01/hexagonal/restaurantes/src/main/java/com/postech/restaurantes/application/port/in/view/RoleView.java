package com.postech.restaurantes.application.port.in.view;

import com.postech.restaurantes.domain.model.Role;

import java.util.UUID;

/** Projeção de saída de um papel de autorização. */
public record RoleView(UUID id, String name) {

    public static RoleView from(Role role) {
        return new RoleView(role.getId(), role.getName().name());
    }
}

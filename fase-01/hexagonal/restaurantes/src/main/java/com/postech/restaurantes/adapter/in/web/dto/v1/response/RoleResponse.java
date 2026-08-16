package com.postech.restaurantes.adapter.in.web.dto.v1.response;

import com.postech.restaurantes.application.port.in.view.RoleView;

import java.util.UUID;

/** DTO de saída para papel/role (v1). */
public record RoleResponse(UUID id, String name) {

    public static RoleResponse from(RoleView view) {
        return new RoleResponse(view.id(), view.name());
    }
}

package com.postech.restaurantes.adapter.in.web.dto.v1.response;

import com.postech.restaurantes.application.port.in.view.AuthView;

/** DTO de saída para autenticação (v1): token, tipo e expiração em ms. */
public record AuthResponse(String token, String type, long expiresIn) {

    public static AuthResponse from(AuthView view) {
        return new AuthResponse(view.token(), view.type(), view.expiresIn());
    }
}

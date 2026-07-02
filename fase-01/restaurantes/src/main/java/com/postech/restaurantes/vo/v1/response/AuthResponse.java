package com.postech.restaurantes.vo.v1.response;

/**
 * VO de saída para autenticação (v1): token JWT, tipo e expiração em ms.
 */
public record AuthResponse(
        String token,
        String type,
        long expiresIn
) {
    public static AuthResponse bearer(String token, long expiresIn) {
        return new AuthResponse(token, "Bearer", expiresIn);
    }
}

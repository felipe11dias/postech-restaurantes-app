package com.postech.restaurantes.vo.v1.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * VO de saída para usuário (v1). Nunca expõe a senha.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String login,
        Set<RoleResponse> roles,
        List<AddressResponse> addresses,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}

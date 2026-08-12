package com.postech.restaurantes.vo.v1.response;

import java.util.UUID;

/**
 * VO de saída para papel/role (v1).
 */
public record RoleResponse(
        UUID id,
        String name
) {
}

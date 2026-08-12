package com.postech.restaurantes.domain.model;

/**
 * Papéis de autorização do sistema.
 * O prefixo ROLE_ segue a convenção do Spring Security
 * (usada por hasRole(...) e pelas GrantedAuthority).
 *
 * ROLE_OWNER    -> Dono de restaurante
 * ROLE_CUSTOMER -> Cliente
 * ROLE_ADMIN    -> Administrador do sistema
 */
public enum RoleName {
    ROLE_OWNER,
    ROLE_CUSTOMER,
    ROLE_ADMIN
}

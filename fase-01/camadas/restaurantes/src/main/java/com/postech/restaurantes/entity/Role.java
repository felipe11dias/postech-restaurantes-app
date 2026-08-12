package com.postech.restaurantes.entity;

import com.postech.restaurantes.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Papel de autorização. Os registros (ROLE_OWNER, ROLE_CUSTOMER, ROLE_ADMIN)
 * são carregados na inicialização (seed) e referenciados pelos usuários
 * através da tabela associativa user_roles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends Auditable {

    private UUID id;
    private RoleName name;
}

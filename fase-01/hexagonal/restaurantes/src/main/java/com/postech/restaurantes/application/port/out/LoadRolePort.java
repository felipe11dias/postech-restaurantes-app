package com.postech.restaurantes.application.port.out;

import com.postech.restaurantes.domain.model.Role;
import com.postech.restaurantes.domain.model.RoleName;

import java.util.Optional;

/** Port de saída: resolução dos papéis de autorização (criados por seed). */
public interface LoadRolePort {

    Optional<Role> findByName(RoleName name);
}

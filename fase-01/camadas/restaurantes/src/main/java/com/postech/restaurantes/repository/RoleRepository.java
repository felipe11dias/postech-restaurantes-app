package com.postech.restaurantes.repository;

import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.enums.RoleName;

import java.util.Optional;

/**
 * Contrato de leitura dos papéis de autorização. Os registros são criados pelo
 * seed da migration V1, então aqui só há consulta.
 */
public interface RoleRepository {

    /** Resolve a entidade Role a partir do nome do papel (usado ao atribuir papéis). */
    Optional<Role> findByName(RoleName name);
}

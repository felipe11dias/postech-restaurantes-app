package com.postech.restaurantes.repository;

import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Resolve a entidade Role a partir do nome do papel (usado ao atribuir papéis). */
    Optional<Role> findByName(RoleName name);
}

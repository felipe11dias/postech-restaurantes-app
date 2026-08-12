package com.postech.restaurantes.repository;

import com.postech.restaurantes.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Unicidade de e-mail (consultado antes do cadastro). */
    boolean existsByEmail(String email);

    /** Unicidade de login. */
    boolean existsByLogin(String login);

    /** Usado na validação de login / autenticação. */
    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    /** Busca paginada de usuários pelo nome (parcial, sem diferenciar maiúsc./minúsc.). */
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

package com.postech.restaurantes.repository;

import com.postech.restaurantes.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência de usuário. Antes era um JpaRepository, que ganhava
 * de graça o CRUD e as consultas derivadas do nome do método; agora cada
 * operação é declarada aqui e implementada em SQL por {@link UserRepositoryJdbc}.
 *
 * O usuário é a raiz do agregado: salvar um usuário também sincroniza seus
 * papéis e endereços, e removê-lo remove ambos em cascata.
 */
public interface UserRepository {

    Optional<User> findById(UUID id);

    /** Usado na validação de login / autenticação. */
    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    boolean existsById(UUID id);

    Page<User> findAll(Pageable pageable);

    /** Busca paginada de usuários pelo nome (parcial, sem diferenciar maiúsc./minúsc.). */
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /** Insere quando o id é nulo, atualiza caso contrário. Devolve a entidade com o id preenchido. */
    User save(User user);

    void deleteById(UUID id);
}

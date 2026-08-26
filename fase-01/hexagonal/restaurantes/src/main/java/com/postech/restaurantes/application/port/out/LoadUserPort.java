package com.postech.restaurantes.application.port.out;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageResult;
import com.postech.restaurantes.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída: leitura de usuários.
 *
 * <p>Separado de {@link SaveUserPort} e de {@link CheckUserExistsPort} de
 * propósito. Um "UserRepository" único obrigaria o caso de uso de exclusão a
 * depender também dos métodos de busca paginada que ele nunca chama, e os testes
 * a criar dublês para métodos irrelevantes. Ports pequenos são o que torna a
 * Segregação de Interfaces concreta na borda de saída.</p>
 */
public interface LoadUserPort {

    Optional<User> findById(UUID id);

    /** Alimenta a autenticação e a autorização por posse do recurso. */
    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    PageResult<User> findAll(PageQuery pageQuery);

    PageResult<User> findByNameContaining(String name, PageQuery pageQuery);
}

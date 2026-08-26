package com.postech.restaurantes.application.port.in;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageResult;
import com.postech.restaurantes.application.port.in.view.UserView;

import java.util.UUID;

/** Caso de uso: consultar usuários — por id, listagem paginada e busca por nome. */
public interface FindUserUseCase {

    UserView findById(UUID userId);

    PageResult<UserView> findAll(PageQuery pageQuery);

    /** Busca paginada por nome (parcial, sem diferenciar maiúsculas/minúsculas). */
    PageResult<UserView> findByName(String name, PageQuery pageQuery);
}

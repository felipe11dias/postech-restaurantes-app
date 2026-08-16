package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageResult;
import com.postech.restaurantes.application.port.in.FindUserUseCase;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;

import java.util.UUID;

/** Consultas de usuário: por id, listagem paginada e busca por nome. */
public class FindUserService implements FindUserUseCase {

    private final LoadUserPort loadUserPort;

    public FindUserService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public UserView findById(UUID userId) {
        return loadUserPort.findById(userId)
                .map(UserView::from)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));
    }

    @Override
    public PageResult<UserView> findAll(PageQuery pageQuery) {
        return loadUserPort.findAll(pageQuery).map(UserView::from);
    }

    @Override
    public PageResult<UserView> findByName(String name, PageQuery pageQuery) {
        // Nome em branco equivale a "sem filtro": é o que o parâmetro opcional
        // ?name= significa para quem chama a API.
        if (name == null || name.isBlank()) {
            return findAll(pageQuery);
        }
        return loadUserPort.findByNameContaining(name.trim(), pageQuery).map(UserView::from);
    }
}

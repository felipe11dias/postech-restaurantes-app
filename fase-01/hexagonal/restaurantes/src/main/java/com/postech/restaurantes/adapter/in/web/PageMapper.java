package com.postech.restaurantes.adapter.in.web;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Tradutor entre a paginação do Spring Data e a paginação do núcleo.
 *
 * <p>Esta classe existe exatamente porque o hexágono não importa {@code Pageable}
 * nem {@code Page}. É um custo real da arquitetura — em camadas, o {@code Pageable}
 * atravessaria do controller ao repositório sem conversão nenhuma. O que se compra
 * com esse custo é a liberdade de trocar a biblioteca de paginação, ou o próprio
 * adapter web, sem tocar em um caso de uso; e um núcleo cujos testes não precisam
 * construir objetos do Spring.</p>
 *
 * <p>A conversão de volta para {@code Page} serve ao {@code PagedResourcesAssembler}
 * do HATEOAS, que monta os links de navegação da coleção.</p>
 */
final class PageMapper {

    private PageMapper() {
    }

    static PageQuery toQuery(Pageable pageable) {
        Sort.Order order = pageable.getSort().stream().findFirst().orElse(null);
        return new PageQuery(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                order == null ? null : order.getProperty(),
                order != null && order.isDescending()
                        ? PageQuery.SortDirection.DESC
                        : PageQuery.SortDirection.ASC);
    }

    static <T> Page<T> toPage(PageResult<T> result, Pageable pageable) {
        return new PageImpl<>(result.content(), pageable, result.totalElements());
    }
}

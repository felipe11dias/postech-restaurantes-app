package com.postech.restaurantes.application.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados devolvida pelo núcleo — a contraparte de saída de
 * {@link PageQuery}, e pelo mesmo motivo: não devolver {@code Page} do Spring
 * Data a partir de dentro do hexágono.
 *
 * @param content       itens da página
 * @param page          número da página devolvida
 * @param size          tamanho pedido
 * @param totalElements total de registros que atendem ao filtro
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), query.page(), query.size(), 0);
    }

    public static <T> PageResult<T> of(List<T> content, PageQuery query, long totalElements) {
        return new PageResult<>(content, query.page(), query.size(), totalElements);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    /** Converte os itens preservando os metadados da página (domínio -> view, view -> DTO). */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}

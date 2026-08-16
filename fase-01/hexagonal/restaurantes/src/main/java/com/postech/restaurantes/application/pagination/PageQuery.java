package com.postech.restaurantes.application.pagination;

/**
 * Pedido de paginação, no vocabulário do núcleo.
 *
 * <p>Existe para que o hexágono não precise importar {@code Pageable} do Spring
 * Data. Paginação é um conceito de negócio ("me devolva a segunda página com 20
 * itens ordenados por nome"); {@code Pageable} é a encarnação desse conceito em
 * um framework específico. O adapter web traduz um para o outro.</p>
 *
 * @param page      número da página, começando em zero
 * @param size      quantidade de itens por página
 * @param sortBy    propriedade de ordenação; o adapter de persistência decide
 *                  quais propriedades aceita e ignora as demais
 * @param direction sentido da ordenação
 */
public record PageQuery(int page, int size, String sortBy, SortDirection direction) {

    private static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("A página não pode ser negativa");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("O tamanho da página deve ser positivo");
        }
        // Teto de segurança: sem ele, ?size=1000000 vira um pedido para carregar
        // a tabela inteira em memória.
        size = Math.min(size, MAX_SIZE);
        direction = direction == null ? SortDirection.ASC : direction;
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, null, SortDirection.ASC);
    }

    public long offset() {
        return (long) page * size;
    }

    public enum SortDirection {
        ASC, DESC
    }
}

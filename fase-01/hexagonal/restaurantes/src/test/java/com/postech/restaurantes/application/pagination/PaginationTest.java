package com.postech.restaurantes.application.pagination;

import com.postech.restaurantes.application.pagination.PageQuery.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PageQuery e PageResult existem para o hexágono não importar Pageable/Page do
 * Spring Data. Como são o vocabulário de paginação do núcleo, é neles que moram
 * o teto de tamanho e o cálculo de páginas — e não no adapter web.
 */
class PaginationTest {

    @Nested
    @DisplayName("PageQuery")
    class Pedido {

        @Test
        @DisplayName("guarda página, tamanho, ordenação e sentido")
        void guardaOsCampos() {
            PageQuery query = new PageQuery(2, 20, "name", SortDirection.DESC);

            assertEquals(2, query.page());
            assertEquals(20, query.size());
            assertEquals("name", query.sortBy());
            assertEquals(SortDirection.DESC, query.direction());
        }

        @Test
        @DisplayName("a fábrica of parte de ASC e sem ordenação declarada")
        void fabricaOf() {
            PageQuery query = PageQuery.of(0, 20);

            assertNull(query.sortBy());
            assertEquals(SortDirection.ASC, query.direction());
        }

        @Test
        @DisplayName("sentido nulo vira ASC")
        void sentidoNuloViraAsc() {
            assertEquals(SortDirection.ASC, new PageQuery(0, 20, "name", null).direction());
        }

        /** Sem o teto, ?size=1000000 seria um pedido para carregar a tabela inteira. */
        @Test
        @DisplayName("tamanho acima do teto é reduzido a 100")
        void tamanhoAcimaDoTeto() {
            assertEquals(100, new PageQuery(0, 1_000_000, null, SortDirection.ASC).size());
        }

        @Test
        @DisplayName("tamanho dentro do teto é preservado")
        void tamanhoDentroDoTeto() {
            assertEquals(20, new PageQuery(0, 20, null, SortDirection.ASC).size());
        }

        @Test
        @DisplayName("página negativa é recusada")
        void paginaNegativa() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PageQuery(-1, 20, null, SortDirection.ASC));
        }

        @Test
        @DisplayName("tamanho zero ou negativo é recusado")
        void tamanhoNaoPositivo() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PageQuery(0, 0, null, SortDirection.ASC));
            assertThrows(IllegalArgumentException.class,
                    () -> new PageQuery(0, -5, null, SortDirection.ASC));
        }

        @Test
        @DisplayName("o deslocamento é página vezes tamanho")
        void deslocamento() {
            assertEquals(40L, new PageQuery(2, 20, null, SortDirection.ASC).offset());
            assertEquals(0L, PageQuery.of(0, 20).offset());
        }

        @Test
        @DisplayName("SortDirection cobre os dois sentidos")
        void sortDirection() {
            assertEquals(2, SortDirection.values().length);
            assertEquals(SortDirection.DESC, SortDirection.valueOf("DESC"));
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Resultado {

        @Test
        @DisplayName("guarda conteúdo, página, tamanho e total")
        void guardaOsCampos() {
            PageResult<String> result = new PageResult<>(List.of("a", "b"), 1, 20, 42);

            assertEquals(List.of("a", "b"), result.content());
            assertEquals(1, result.page());
            assertEquals(20, result.size());
            assertEquals(42L, result.totalElements());
        }

        /** Cópia defensiva: a página devolvida não muda se a origem mudar. */
        @Test
        @DisplayName("o conteúdo é copiado e fica imutável")
        void conteudoImutavel() {
            List<String> origem = new ArrayList<>(List.of("a"));

            PageResult<String> result = new PageResult<>(origem, 0, 20, 1);
            origem.clear();

            assertEquals(1, result.content().size());
            assertThrows(UnsupportedOperationException.class, () -> result.content().add("b"));
        }

        @Test
        @DisplayName("conteúdo nulo vira lista vazia")
        void conteudoNulo() {
            assertTrue(new PageResult<String>(null, 0, 20, 0).content().isEmpty());
        }

        @Test
        @DisplayName("empty devolve página vazia preservando o pedido")
        void paginaVazia() {
            PageResult<String> result = PageResult.empty(new PageQuery(3, 20, null, SortDirection.ASC));

            assertTrue(result.content().isEmpty());
            assertEquals(3, result.page());
            assertEquals(20, result.size());
            assertEquals(0L, result.totalElements());
        }

        @Test
        @DisplayName("of monta a página a partir do pedido e do total")
        void ofMontaAPagina() {
            PageResult<String> result = PageResult.of(List.of("a"), PageQuery.of(1, 20), 21);

            assertEquals(List.of("a"), result.content());
            assertEquals(1, result.page());
            assertEquals(21L, result.totalElements());
        }

        @Test
        @DisplayName("o total de páginas arredonda para cima")
        void totalDePaginas() {
            assertEquals(3, new PageResult<>(List.of(), 0, 20, 41).totalPages());
            assertEquals(2, new PageResult<>(List.of(), 0, 20, 40).totalPages());
            assertEquals(0, new PageResult<>(List.of(), 0, 20, 0).totalPages());
        }

        /** Guarda contra divisão por zero — o PageQuery já impede size zero. */
        @Test
        @DisplayName("tamanho zero resulta em zero páginas, e não em erro")
        void tamanhoZero() {
            assertEquals(0, new PageResult<>(List.of(), 0, 0, 10).totalPages());
        }

        @Test
        @DisplayName("map converte os itens preservando os metadados da página")
        void mapPreservaMetadados() {
            PageResult<Integer> convertido =
                    new PageResult<>(List.of("a", "bb"), 1, 20, 42).map(String::length);

            assertEquals(List.of(1, 2), convertido.content());
            assertEquals(1, convertido.page());
            assertEquals(20, convertido.size());
            assertEquals(42L, convertido.totalElements());
        }
    }
}

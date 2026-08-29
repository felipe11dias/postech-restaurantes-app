package com.postech.restaurantes.adapter.in.web;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageQuery.SortDirection;
import com.postech.restaurantes.application.pagination.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Este tradutor existe porque o hexágono não importa {@code Pageable} nem
 * {@code Page}. É um custo real da arquitetura — e é justamente por ser a única
 * ponte entre os dois vocabulários que ele precisa estar coberto: um erro aqui
 * corromperia toda paginação da API sem que nenhum caso de uso percebesse.
 */
@DisplayName("PageMapper — tradução entre a paginação do Spring Data e a do núcleo")
class PageMapperTest {

    @Test
    @DisplayName("converte página, tamanho e a primeira ordenação declarada")
    void converteOrdenacaoAscendente() {
        PageQuery query = PageMapper.toQuery(PageRequest.of(2, 20, Sort.by("name")));

        assertEquals(2, query.page());
        assertEquals(20, query.size());
        assertEquals("name", query.sortBy());
        assertEquals(SortDirection.ASC, query.direction());
    }

    @Test
    @DisplayName("converte ordenação decrescente")
    void converteOrdenacaoDecrescente() {
        PageQuery query = PageMapper.toQuery(
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"))));

        assertEquals("createdAt", query.sortBy());
        assertEquals(SortDirection.DESC, query.direction());
    }

    /** Só a primeira ordenação atravessa: o núcleo aceita uma propriedade. */
    @Test
    @DisplayName("com várias ordenações, só a primeira atravessa")
    void apenasAPrimeiraOrdenacao() {
        PageQuery query = PageMapper.toQuery(PageRequest.of(0, 20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("name"))));

        assertEquals("createdAt", query.sortBy());
        assertEquals(SortDirection.DESC, query.direction());
    }

    @Test
    @DisplayName("Pageable sem ordenação vira consulta sem ordenação declarada")
    void semOrdenacao() {
        PageQuery query = PageMapper.toQuery(PageRequest.of(0, 20, Sort.unsorted()));

        assertNull(query.sortBy());
        assertEquals(SortDirection.ASC, query.direction());
    }

    @Test
    @DisplayName("a volta para Page preserva conteúdo, paginação e total")
    void voltaParaPage() {
        PageResult<String> resultado = new PageResult<>(List.of("a", "b"), 1, 20, 42);

        Page<String> page = PageMapper.toPage(resultado, PageRequest.of(1, 20));

        assertEquals(List.of("a", "b"), page.getContent());
        assertEquals(1, page.getNumber());
        assertEquals(20, page.getSize());
        assertEquals(42L, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
    }

    @Test
    @DisplayName("página vazia atravessa como página vazia")
    void paginaVazia() {
        Page<String> page = PageMapper.toPage(
                PageResult.empty(PageQuery.of(0, 20)), PageRequest.of(0, 20));

        assertEquals(0L, page.getTotalElements());
        assertFalse(page.hasContent());
    }

    /** Classe utilitária: existe só para as duas conversões estáticas. */
    @Test
    @DisplayName("não é instanciável fora da própria classe")
    void naoInstanciavel() throws Exception {
        Constructor<PageMapper> constructor = PageMapper.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null));

        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}

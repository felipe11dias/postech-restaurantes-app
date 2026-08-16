package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageResult;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FindUserServiceTest {

    private static final PageQuery PAGINA = PageQuery.of(0, 20);

    @Mock
    private LoadUserPort loadUserPort;

    private FindUserService service;

    @BeforeEach
    void setUp() {
        service = new FindUserService(loadUserPort);
    }

    @Test
    @DisplayName("devolve o usuário encontrado como view, sem a senha")
    void encontraPorId() {
        UUID id = UUID.randomUUID();
        given(loadUserPort.findById(id)).willReturn(Optional.of(DomainFixtures.usuarioPersistido(id)));

        UserView view = service.findById(id);

        assertEquals(id, view.id());
        assertEquals("maria@email.com", view.email());
    }

    @Test
    @DisplayName("falha quando o id não existe")
    void falhaQuandoNaoEncontra() {
        UUID id = UUID.randomUUID();
        given(loadUserPort.findById(id)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    @DisplayName("busca por nome preserva os metadados da página")
    void buscaPorNomePreservaMetadados() {
        List<User> usuarios = List.of(DomainFixtures.usuarioPersistido());
        given(loadUserPort.findByNameContaining("maria", PAGINA))
                .willReturn(PageResult.of(usuarios, PAGINA, 42));

        PageResult<UserView> resultado = service.findByName("maria", PAGINA);

        assertEquals(1, resultado.content().size());
        assertEquals(42, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    @DisplayName("nome em branco equivale a listar tudo")
    void nomeEmBrancoListaTudo() {
        given(loadUserPort.findAll(PAGINA)).willReturn(PageResult.empty(PAGINA));

        service.findByName("   ", PAGINA);

        verify(loadUserPort).findAll(PAGINA);
        verify(loadUserPort, never()).findByNameContaining(anyString(), any());
    }

    @Test
    @DisplayName("nome nulo equivale a listar tudo")
    void nomeNuloListaTudo() {
        given(loadUserPort.findAll(PAGINA)).willReturn(PageResult.empty(PAGINA));

        service.findByName(null, PAGINA);

        verify(loadUserPort).findAll(PAGINA);
    }
}

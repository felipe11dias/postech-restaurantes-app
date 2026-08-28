package com.postech.restaurantes.adapter.in.web.assembler;

import com.postech.restaurantes.adapter.in.web.dto.v1.response.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os links são montados a partir da requisição em curso, então o teste precisa de
 * um contexto de requisição — é ele que dá base ao endereço absoluto.
 */
@DisplayName("UserModelAssembler — links HATEOAS da resposta de usuário")
class UserModelAssemblerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private final UserModelAssembler assembler = new UserModelAssembler();

    @BeforeEach
    void abrirContextoDeRequisicao() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void limparContextoDeRequisicao() {
        RequestContextHolder.resetRequestAttributes();
    }

    private UserResponse usuario() {
        return new UserResponse(USER_ID, "Maria Silva", "maria@email.com", "maria.silva",
                Set.of(), List.of(), null, null);
    }

    @Test
    @DisplayName("o link self aponta para o próprio usuário")
    void linkSelf() {
        EntityModel<UserResponse> model = assembler.toModel(usuario());

        assertTrue(model.getRequiredLink(IanaLinkRelations.SELF).getHref()
                .endsWith("/api/v1/users/" + USER_ID));
    }

    @Test
    @DisplayName("o link users aponta para a coleção")
    void linkDaColecao() {
        EntityModel<UserResponse> model = assembler.toModel(usuario());

        assertTrue(model.getRequiredLink("users").getHref().endsWith("/api/v1/users"));
    }

    /** Os DTOs continuam records imutáveis: os links ficam no envelope. */
    @Test
    @DisplayName("os links ficam no envelope, e não dentro do record")
    void linksNoEnvelope() {
        UserResponse response = usuario();

        EntityModel<UserResponse> model = assembler.toModel(response);

        assertSame(response, model.getContent());
        assertEquals(2, model.getLinks().stream().count());
    }
}

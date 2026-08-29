package com.postech.restaurantes.assembler;

import com.postech.restaurantes.vo.v1.response.UserResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os links são montados a partir da requisição em curso, então o teste precisa
 * de um contexto de requisição — é ele que dá base ao endereço absoluto.
 */
@DisplayName("UserModelAssembler — links HATEOAS da resposta de usuário")
class UserModelAssemblerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

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
        return new UserResponse(USER_ID, "João Silva", "joao@email.com", "joao.silva",
                Set.of(), List.of(), null, null);
    }

    @Test
    @DisplayName("o link self aponta para o próprio usuário")
    void toModel_deveGerarLinkSelf() {
        EntityModel<UserResponse> model = assembler.toModel(usuario());

        assertThat(model.getContent()).isEqualTo(usuario());
        assertThat(model.getRequiredLink(IanaLinkRelations.SELF).getHref())
                .endsWith("/api/v1/users/" + USER_ID);
    }

    @Test
    @DisplayName("o link users aponta para a coleção")
    void toModel_deveGerarLinkDaColecao() {
        EntityModel<UserResponse> model = assembler.toModel(usuario());

        assertThat(model.getRequiredLink("users").getHref()).endsWith("/api/v1/users");
    }

    /** Os VOs continuam imutáveis: os links ficam no envelope, não no record. */
    @Test
    @DisplayName("os links ficam no envelope, e não dentro do VO")
    void toModel_naoDeveAlterarOVo() {
        UserResponse response = usuario();

        EntityModel<UserResponse> model = assembler.toModel(response);

        assertThat(model.getContent()).isSameAs(response);
        assertThat(model.getLinks()).hasSize(2);
    }
}

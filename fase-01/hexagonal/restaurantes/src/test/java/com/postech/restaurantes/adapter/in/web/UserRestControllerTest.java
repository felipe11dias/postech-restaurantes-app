package com.postech.restaurantes.adapter.in.web;

import com.postech.restaurantes.adapter.in.web.assembler.UserModelAssembler;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.UserUpdateRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.UserResponse;
import com.postech.restaurantes.application.pagination.PageQuery;
import com.postech.restaurantes.application.pagination.PageResult;
import com.postech.restaurantes.application.port.in.ChangePasswordUseCase;
import com.postech.restaurantes.application.port.in.DeleteUserUseCase;
import com.postech.restaurantes.application.port.in.FindUserUseCase;
import com.postech.restaurantes.application.port.in.RegisterUserUseCase;
import com.postech.restaurantes.application.port.in.UpdateUserUseCase;
import com.postech.restaurantes.application.port.in.command.ChangePasswordCommand;
import com.postech.restaurantes.application.port.in.command.RegisterUserCommand;
import com.postech.restaurantes.application.port.in.command.UpdateUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.domain.model.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * O controller depende de cinco <em>interfaces de caso de uso</em>, nenhuma classe
 * concreta — é onde a inversão de dependência se materializa na borda de entrada.
 * Por isso o teste não precisa de contexto Spring: dublar os ports basta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRestController — endpoints de usuário")
class UserRestControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private RegisterUserUseCase registerUserUseCase;
    @Mock
    private UpdateUserUseCase updateUserUseCase;
    @Mock
    private ChangePasswordUseCase changePasswordUseCase;
    @Mock
    private DeleteUserUseCase deleteUserUseCase;
    @Mock
    private FindUserUseCase findUserUseCase;
    @Mock
    private UserModelAssembler assembler;
    @Mock
    private PagedResourcesAssembler<UserResponse> pagedAssembler;

    @InjectMocks
    private UserRestController controller;

    private UserView view() {
        return new UserView(USER_ID, "Maria Silva", "maria@email.com", "maria.silva",
                Set.of(), List.of(), null, null);
    }

    private EntityModel<UserResponse> modelo() {
        return EntityModel.of(UserResponse.from(view()),
                Link.of("http://localhost/api/v1/users/" + USER_ID).withSelfRel(),
                Link.of("http://localhost/api/v1/users").withRel("users"));
    }

    private UserRegistrationRequest cadastro() {
        return new UserRegistrationRequest("Maria Silva", "maria@email.com", "maria.silva",
                "senha12345", Set.of(RoleName.ROLE_CUSTOMER), List.of());
    }

    @Test
    @DisplayName("cadastro devolve 201 com Location apontando para o novo recurso")
    void register() {
        given(registerUserUseCase.register(any(RegisterUserCommand.class))).willReturn(view());
        given(assembler.toModel(any(UserResponse.class))).willReturn(modelo());

        ResponseEntity<EntityModel<UserResponse>> response = controller.register(cadastro());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("http://localhost/api/v1/users/" + USER_ID,
                response.getHeaders().getLocation().toString());
        assertEquals(USER_ID, response.getBody().getContent().id());
    }

    @Test
    @DisplayName("consulta por id devolve o usuário com links")
    void findById() {
        given(findUserUseCase.findById(USER_ID)).willReturn(view());
        given(assembler.toModel(any(UserResponse.class))).willReturn(modelo());

        EntityModel<UserResponse> model = controller.findById(USER_ID);

        assertEquals(USER_ID, model.getContent().id());
        assertEquals(2, model.getLinks().stream().count());
    }

    @Test
    @DisplayName("listagem traduz o Pageable, chama o caso de uso e devolve o PagedModel")
    void list() {
        Pageable pageable = PageRequest.of(1, 20, Sort.by("name"));
        PagedModel<EntityModel<UserResponse>> esperado = PagedModel.of(List.of(modelo()),
                new PagedModel.PageMetadata(20, 1, 1));
        given(findUserUseCase.findByName(eq("Ma"), any(PageQuery.class)))
                .willReturn(new PageResult<>(List.of(view()), 1, 20, 1));
        given(pagedAssembler.toModel(any(Page.class), eq(assembler))).willReturn(esperado);

        assertSame(esperado, controller.list("Ma", pageable, pagedAssembler));

        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(findUserUseCase).findByName(eq("Ma"), captor.capture());
        assertEquals(1, captor.getValue().page());
        assertEquals(20, captor.getValue().size());
        assertEquals("name", captor.getValue().sortBy());
    }

    @Test
    @DisplayName("listagem sem filtro de nome também chega ao caso de uso")
    void listSemNome() {
        given(findUserUseCase.findByName(eq(null), any(PageQuery.class)))
                .willReturn(new PageResult<>(List.of(), 0, 20, 0));

        controller.list(null, PageRequest.of(0, 20, Sort.by("name")), pagedAssembler);

        verify(findUserUseCase).findByName(eq(null), any(PageQuery.class));
    }

    @Test
    @DisplayName("atualização devolve o usuário atualizado, com o id vindo do path")
    void update() {
        given(updateUserUseCase.update(any(UpdateUserCommand.class))).willReturn(view());
        given(assembler.toModel(any(UserResponse.class))).willReturn(modelo());

        EntityModel<UserResponse> model = controller.update(USER_ID,
                new UserUpdateRequest("Maria Silva", "maria@email.com", "maria.silva", List.of()));

        assertEquals(USER_ID, model.getContent().id());

        ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
        verify(updateUserUseCase).update(captor.capture());
        assertEquals(USER_ID, captor.getValue().userId());
    }

    @Test
    @DisplayName("troca de senha devolve 204 sem corpo")
    void changePassword() {
        ResponseEntity<Void> response = controller.changePassword(USER_ID,
                new PasswordChangeRequest("atual123", "nova12345", "nova12345"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        ArgumentCaptor<ChangePasswordCommand> captor =
                ArgumentCaptor.forClass(ChangePasswordCommand.class);
        verify(changePasswordUseCase).changePassword(captor.capture());
        assertEquals(USER_ID, captor.getValue().userId());
        assertEquals("atual123", captor.getValue().currentPassword());
    }

    @Test
    @DisplayName("exclusão devolve 204 sem corpo")
    void delete() {
        ResponseEntity<Void> response = controller.delete(USER_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(deleteUserUseCase).delete(USER_ID);
    }
}

package com.postech.restaurantes.controller;

import com.postech.restaurantes.assembler.UserModelAssembler;
import com.postech.restaurantes.enums.RoleName;
import com.postech.restaurantes.service.UserService;
import com.postech.restaurantes.vo.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.vo.v1.request.UserUpdateRequest;
import com.postech.restaurantes.vo.v1.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController — endpoints de usuário")
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Mock private UserService userService;
    @Mock private UserModelAssembler assembler;
    @Mock private PagedResourcesAssembler<UserResponse> pagedAssembler;

    @InjectMocks private UserController userController;

    private UserResponse usuario() {
        return new UserResponse(USER_ID, "João Silva", "joao@email.com", "joao.silva",
                Set.of(), List.of(), null, null);
    }

    private EntityModel<UserResponse> modelo() {
        return EntityModel.of(usuario(),
                Link.of("http://localhost/api/v1/users/" + USER_ID).withSelfRel(),
                Link.of("http://localhost/api/v1/users").withRel("users"));
    }

    @Test
    @DisplayName("cadastro devolve 201 com Location apontando para o novo recurso")
    void register_deveDevolver201ComLocation() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "João Silva", "joao@email.com", "joao.silva", "senhaSegura123",
                Set.of(RoleName.ROLE_CUSTOMER), List.of());
        when(userService.register(request)).thenReturn(usuario());
        when(assembler.toModel(usuario())).thenReturn(modelo());

        ResponseEntity<EntityModel<UserResponse>> response = userController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost/api/v1/users/" + USER_ID);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEqualTo(usuario());
    }

    @Test
    @DisplayName("consulta por id devolve o usuário com links")
    void findById_deveDevolverOUsuarioComLinks() {
        when(userService.findById(USER_ID)).thenReturn(usuario());
        when(assembler.toModel(usuario())).thenReturn(modelo());

        EntityModel<UserResponse> model = userController.findById(USER_ID);

        assertThat(model.getContent()).isEqualTo(usuario());
        assertThat(model.getLinks()).hasSize(2);
    }

    @Test
    @DisplayName("listagem sem filtro devolve a página completa")
    void list_semFiltro_deveListarTodos() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserResponse> pagina = new PageImpl<>(List.of(usuario()));
        PagedModel<EntityModel<UserResponse>> esperado = PagedModel.of(List.of(modelo()),
                new PagedModel.PageMetadata(20, 0, 1));
        when(userService.findAll(pageable)).thenReturn(pagina);
        when(pagedAssembler.toModel(pagina, assembler)).thenReturn(esperado);

        assertThat(userController.list(null, pageable, pagedAssembler)).isSameAs(esperado);
    }

    @Test
    @DisplayName("listagem com nome em branco também lista todos")
    void list_comNomeEmBranco_deveListarTodos() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserResponse> pagina = new PageImpl<>(List.of(usuario()));
        when(userService.findAll(pageable)).thenReturn(pagina);

        userController.list("   ", pageable, pagedAssembler);

        verify(userService).findAll(pageable);
    }

    @Test
    @DisplayName("listagem com nome informado faz a busca por nome")
    void list_comNome_deveBuscarPorNome() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserResponse> pagina = new PageImpl<>(List.of(usuario()));
        when(userService.findByName("jo", pageable)).thenReturn(pagina);

        userController.list("jo", pageable, pagedAssembler);

        verify(userService).findByName("jo", pageable);
    }

    @Test
    @DisplayName("atualização devolve o usuário atualizado com links")
    void update_deveDevolverOUsuarioAtualizado() {
        UserUpdateRequest request =
                new UserUpdateRequest("João Silva", "joao@email.com", "joao.silva", List.of());
        when(userService.update(USER_ID, request)).thenReturn(usuario());
        when(assembler.toModel(usuario())).thenReturn(modelo());

        EntityModel<UserResponse> model = userController.update(USER_ID, request);

        assertThat(model.getContent()).isEqualTo(usuario());
    }

    @Test
    @DisplayName("troca de senha devolve 204 sem corpo")
    void changePassword_deveDevolver204() {
        PasswordChangeRequest request =
                new PasswordChangeRequest("atual123", "nova12345", "nova12345");

        ResponseEntity<Void> response = userController.changePassword(USER_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(userService).changePassword(USER_ID, request);
        verifyNoInteractions(assembler);
    }

    @Test
    @DisplayName("exclusão devolve 204 sem corpo")
    void delete_deveDevolver204() {
        ResponseEntity<Void> response = userController.delete(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(userService).delete(USER_ID);
    }
}

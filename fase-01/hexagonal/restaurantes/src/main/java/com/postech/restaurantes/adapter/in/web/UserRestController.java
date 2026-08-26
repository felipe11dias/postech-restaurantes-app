package com.postech.restaurantes.adapter.in.web;

import com.postech.restaurantes.adapter.in.web.assembler.UserModelAssembler;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.UserUpdateRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.UserResponse;
import com.postech.restaurantes.application.port.in.ChangePasswordUseCase;
import com.postech.restaurantes.application.port.in.DeleteUserUseCase;
import com.postech.restaurantes.application.port.in.FindUserUseCase;
import com.postech.restaurantes.application.port.in.RegisterUserUseCase;
import com.postech.restaurantes.application.port.in.UpdateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Adapter de entrada REST para os casos de uso de usuário.
 *
 * <p>Repare nas dependências do construtor: são cinco <em>interfaces de caso de
 * uso</em>, nenhuma classe concreta. O controller não sabe que existe um
 * {@code RegisterUserService} — sabe apenas que alguém honra o contrato
 * {@code RegisterUserUseCase}. Esse é o ponto exato onde a inversão de dependência
 * se materializa na borda de entrada.</p>
 *
 * <p>Cinco dependências também documentam algo: este controller expõe cinco
 * capacidades distintas. Em uma camada de serviço monolítica isso ficaria
 * escondido atrás de uma única injeção de {@code UserService}.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Cadastro, consulta, atualização e exclusão de usuários")
public class UserRestController {

    /** Autorização por posse: o próprio usuário (pelo id) ou um administrador. */
    private static final String OWNER_OR_ADMIN =
            "hasRole('ADMIN') or @resourceOwner.isSelf(#id, authentication)";

    private final RegisterUserUseCase registerUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final FindUserUseCase findUserUseCase;
    private final UserModelAssembler assembler;

    public UserRestController(RegisterUserUseCase registerUserUseCase,
                              UpdateUserUseCase updateUserUseCase,
                              ChangePasswordUseCase changePasswordUseCase,
                              DeleteUserUseCase deleteUserUseCase,
                              FindUserUseCase findUserUseCase,
                              UserModelAssembler assembler) {
        this.registerUserUseCase = registerUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.findUserUseCase = findUserUseCase;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Cadastra um usuário (endpoint público de autocadastro)")
    public ResponseEntity<EntityModel<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = UserResponse.from(registerUserUseCase.register(request.toCommand()));
        EntityModel<UserResponse> model = assembler.toModel(response);
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @Operation(summary = "Consulta um usuário pelo id")
    public EntityModel<UserResponse> findById(@PathVariable UUID id) {
        return assembler.toModel(UserResponse.from(findUserUseCase.findById(id)));
    }

    /**
     * Lista paginada de usuários; com o parâmetro {@code name}, faz busca paginada
     * por nome. Aceita os parâmetros de paginação {@code page}, {@code size} e {@code sort}.
     */
    @GetMapping
    @Operation(summary = "Lista usuários, opcionalmente filtrando por nome")
    public PagedModel<EntityModel<UserResponse>> list(
            @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            PagedResourcesAssembler<UserResponse> pagedAssembler) {

        Page<UserResponse> page = PageMapper.toPage(
                findUserUseCase.findByName(name, PageMapper.toQuery(pageable))
                        .map(UserResponse::from),
                pageable);

        return pagedAssembler.toModel(page, assembler);
    }

    /** Atualização das demais informações do usuário (endpoint distinto do de senha). */
    @PutMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @Operation(summary = "Atualiza os dados cadastrais do usuário (não altera a senha)")
    public EntityModel<UserResponse> update(@PathVariable UUID id,
                                            @Valid @RequestBody UserUpdateRequest request) {
        return assembler.toModel(UserResponse.from(updateUserUseCase.update(request.toCommand(id))));
    }

    /** Troca de senha (endpoint exclusivo). */
    @PatchMapping("/{id}/password")
    @PreAuthorize(OWNER_OR_ADMIN)
    @Operation(summary = "Troca a senha do usuário informando a senha atual")
    public ResponseEntity<Void> changePassword(@PathVariable UUID id,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        changePasswordUseCase.changePassword(request.toCommand(id));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @Operation(summary = "Exclui um usuário")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUserUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.postech.restaurantes.controller;

import com.postech.restaurantes.service.UserService;
import com.postech.restaurantes.vo.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.vo.v1.request.UserUpdateRequest;
import com.postech.restaurantes.vo.v1.response.UserResponse;
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

/**
 * Endpoints de usuário, versionados sob /api/v1/users.
 * Cumpre os requisitos: cadastro, atualização de dados (endpoint distinto),
 * troca de senha (endpoint exclusivo), exclusão e busca por nome.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /** Autorização por posse: o próprio usuário (pelo id) ou um administrador. */
    private static final String OWNER_OR_ADMIN =
            "hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication)";

    private final UserService userService;
    private final UserModelAssembler assembler;

    public UserController(UserService userService, UserModelAssembler assembler) {
        this.userService = userService;
        this.assembler = assembler;
    }

    @PostMapping
    public ResponseEntity<EntityModel<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        EntityModel<UserResponse> model = assembler.toModel(userService.register(request));
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    public EntityModel<UserResponse> findById(@PathVariable Long id) {
        return assembler.toModel(userService.findById(id));
    }

    /**
     * Lista paginada de usuários; se informado o parâmetro `name`, faz busca
     * paginada por nome. Aceita os parâmetros de paginação `page`, `size` e `sort`.
     */
    @GetMapping
    public PagedModel<EntityModel<UserResponse>> list(
            @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            PagedResourcesAssembler<UserResponse> pagedAssembler) {
        Page<UserResponse> page = (name == null || name.isBlank())
                ? userService.findAll(pageable)
                : userService.findByName(name, pageable);

        return pagedAssembler.toModel(page, assembler);
    }

    /** Atualização das demais informações do usuário (endpoint distinto da senha). */
    @PutMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    public EntityModel<UserResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UserUpdateRequest request) {
        return assembler.toModel(userService.update(id, request));
    }

    /** Troca de senha (endpoint exclusivo). */
    @PatchMapping("/{id}/password")
    @PreAuthorize(OWNER_OR_ADMIN)
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.postech.restaurantes.controller;

import com.postech.restaurantes.vo.v1.response.UserResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Converte um UserResponse em EntityModel com links HATEOAS (self e coleção),
 * mantendo a lógica de hipermídia fora do controller (SRP). Os VOs permanecem
 * imutáveis — os links ficam no envelope EntityModel, não no record.
 */
@Component
public class UserModelAssembler
        implements RepresentationModelAssembler<UserResponse, EntityModel<UserResponse>> {

    @Override
    public EntityModel<UserResponse> toModel(UserResponse user) {
        return EntityModel.of(user,
                linkTo(methodOn(UserController.class).findById(user.id())).withSelfRel(),
                linkTo(UserController.class).withRel("users"));
    }
}

package com.postech.restaurantes.adapter.in.web.assembler;

import com.postech.restaurantes.adapter.in.web.UserRestController;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.UserResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Converte um {@code UserResponse} em {@code EntityModel} com links HATEOAS
 * (self e coleção), mantendo a lógica de hipermídia fora do controller.
 *
 * <p>Os DTOs permanecem records imutáveis: os links ficam no envelope
 * {@code EntityModel}, não dentro do record.</p>
 */
@Component
public class UserModelAssembler
        implements RepresentationModelAssembler<UserResponse, EntityModel<UserResponse>> {

    @Override
    public EntityModel<UserResponse> toModel(UserResponse user) {
        return EntityModel.of(user,
                linkTo(methodOn(UserRestController.class).findById(user.id())).withSelfRel(),
                linkTo(UserRestController.class).withRel("users"));
    }
}

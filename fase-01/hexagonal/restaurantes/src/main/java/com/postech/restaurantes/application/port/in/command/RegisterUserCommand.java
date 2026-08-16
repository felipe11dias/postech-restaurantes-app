package com.postech.restaurantes.application.port.in.command;

import com.postech.restaurantes.domain.model.RoleName;

import java.util.List;
import java.util.Set;

/**
 * Comando de cadastro de usuário.
 *
 * <p>Os <em>commands</em> são a fronteira de entrada do hexágono: objetos
 * imutáveis, independentes de HTTP, que descrevem a intenção em termos de negócio.
 * Um segundo adapter de entrada (uma CLI, um consumidor de fila) chamaria o mesmo
 * caso de uso montando o mesmo command — é isso que impede o formato do JSON de
 * virar, na prática, a assinatura da regra de negócio.</p>
 *
 * @param rawPassword senha em claro; será convertida em hash pelo adapter de
 *                    segurança antes de chegar ao domínio
 */
public record RegisterUserCommand(
        String name,
        String email,
        String login,
        String rawPassword,
        Set<RoleName> roles,
        List<AddressCommand> addresses
) {
}

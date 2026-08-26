package com.postech.restaurantes.application.port.in.command;

import java.util.List;
import java.util.UUID;

/**
 * Comando de atualização dos dados cadastrais.
 *
 * <p>Não tem campo de senha, e isso é intencional: a troca de senha é um caso de
 * uso separado, com command próprio. A separação começa aqui, no tipo — não
 * apenas na rota HTTP.</p>
 */
public record UpdateUserCommand(
        UUID userId,
        String name,
        String email,
        String login,
        List<AddressCommand> addresses
) {
}

package com.postech.restaurantes.application.port.in.command;

/** Solicitação de recuperação de senha a partir do e-mail cadastrado. */
public record RequestPasswordResetCommand(String email) {
}

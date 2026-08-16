package com.postech.restaurantes.application.port.in.command;

/** Credenciais apresentadas na validação de login. */
public record AuthenticateCommand(String login, String rawPassword) {
}

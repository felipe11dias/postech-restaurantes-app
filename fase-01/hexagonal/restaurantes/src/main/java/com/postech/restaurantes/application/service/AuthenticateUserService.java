package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.AuthenticateUseCase;
import com.postech.restaurantes.application.port.in.command.AuthenticateCommand;
import com.postech.restaurantes.application.port.in.view.AuthView;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.TokenProviderPort;
import com.postech.restaurantes.domain.exception.AuthenticationFailedException;
import com.postech.restaurantes.domain.model.User;

/**
 * Validação de credenciais e emissão do token de acesso.
 *
 * <p>Este é o caso de uso onde a diferença entre as duas variantes fica mais
 * visível. Na versão em camadas, autenticar é delegado ao
 * {@code AuthenticationManager} do Spring Security — a regra vive dentro do
 * framework. Aqui, o caso de uso a executa explicitamente sobre dois ports
 * (carregar o usuário, conferir o hash) e só então pede um token a um terceiro.
 * O resultado é uma regra de negócio legível e testável sem subir nada.</p>
 *
 * <p>Usuário inexistente e senha errada produzem exatamente a mesma exceção, de
 * propósito: respostas distintas transformariam o endpoint de login em uma forma
 * de descobrir quais contas existem.</p>
 */
public class AuthenticateUserService implements AuthenticateUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;

    public AuthenticateUserService(LoadUserPort loadUserPort,
                                   PasswordEncoderPort passwordEncoderPort,
                                   TokenProviderPort tokenProviderPort) {
        this.loadUserPort = loadUserPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    @Override
    public AuthView authenticate(AuthenticateCommand command) {
        User user = loadUserPort.findByLogin(command.login())
                .orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoderPort.matches(command.rawPassword(), user.getPassword())) {
            throw new AuthenticationFailedException();
        }

        return AuthView.bearer(
                tokenProviderPort.generateToken(user),
                tokenProviderPort.expirationInMillis());
    }
}

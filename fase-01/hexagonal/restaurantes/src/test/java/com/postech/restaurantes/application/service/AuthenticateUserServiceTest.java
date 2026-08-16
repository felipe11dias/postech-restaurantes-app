package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.command.AuthenticateCommand;
import com.postech.restaurantes.application.port.in.view.AuthView;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.TokenProviderPort;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.exception.AuthenticationFailedException;
import com.postech.restaurantes.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Testes da autenticação.
 *
 * <p>Na variante em camadas este caso de uso delega ao {@code AuthenticationManager}
 * do Spring Security e, para ser testado, exigiria contexto ou um mock do
 * framework. Aqui a regra é do caso de uso, então o teste a exercita diretamente.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @Mock
    private TokenProviderPort tokenProviderPort;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(loadUserPort, passwordEncoderPort, tokenProviderPort);
    }

    @Test
    @DisplayName("emite token Bearer quando as credenciais conferem")
    void emiteTokenComCredenciaisValidas() {
        User user = DomainFixtures.usuarioPersistido();
        given(loadUserPort.findByLogin("maria.silva")).willReturn(Optional.of(user));
        given(passwordEncoderPort.matches("senha12345", DomainFixtures.SENHA_HASH)).willReturn(true);
        given(tokenProviderPort.generateToken(user)).willReturn("token-emitido");
        given(tokenProviderPort.expirationInMillis()).willReturn(3600000L);

        AuthView view = service.authenticate(new AuthenticateCommand("maria.silva", "senha12345"));

        assertEquals("token-emitido", view.token());
        assertEquals("Bearer", view.type());
        assertEquals(3600000L, view.expiresIn());
    }

    @Test
    @DisplayName("recusa senha incorreta sem emitir token")
    void recusaSenhaIncorreta() {
        given(loadUserPort.findByLogin(anyString()))
                .willReturn(Optional.of(DomainFixtures.usuarioPersistido()));
        given(passwordEncoderPort.matches(anyString(), anyString())).willReturn(false);

        assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate(new AuthenticateCommand("maria.silva", "errada")));
        verify(tokenProviderPort, never()).generateToken(any());
    }

    @Test
    @DisplayName("login inexistente e senha errada falham do mesmo jeito — não revelam quais contas existem")
    void loginInexistenteFalhaIgual() {
        given(loadUserPort.findByLogin("nao.existe")).willReturn(Optional.empty());

        AuthenticationFailedException inexistente = assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate(new AuthenticateCommand("nao.existe", "qualquer")));

        assertEquals("Login ou senha inválidos", inexistente.getMessage());
        verify(tokenProviderPort, never()).generateToken(any());
    }
}

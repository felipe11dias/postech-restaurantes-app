package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.command.ChangePasswordCommand;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String NOVO_HASH = "$2a$10$novohash";

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveUserPort saveUserPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private ChangePasswordService service;

    @BeforeEach
    void setUp() {
        service = new ChangePasswordService(loadUserPort, saveUserPort, passwordEncoderPort,
                new DirectTransactionPort());
    }

    @Test
    @DisplayName("troca a senha quando a atual confere e a confirmação coincide")
    void trocaComSucesso() {
        User user = DomainFixtures.usuarioPersistido(USER_ID);
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoderPort.matches("senhaAtual", DomainFixtures.SENHA_HASH)).willReturn(true);
        given(passwordEncoderPort.encode("novaSenha123")).willReturn(NOVO_HASH);

        service.changePassword(new ChangePasswordCommand(
                USER_ID, "senhaAtual", "novaSenha123", "novaSenha123"));

        assertEquals(NOVO_HASH, user.getPassword());
        verify(saveUserPort).save(user);
    }

    @Test
    @DisplayName("recusa quando a senha atual está incorreta")
    void recusaSenhaAtualIncorreta() {
        given(loadUserPort.findById(USER_ID))
                .willReturn(Optional.of(DomainFixtures.usuarioPersistido(USER_ID)));
        given(passwordEncoderPort.matches(anyString(), anyString())).willReturn(false);

        assertThrows(InvalidPasswordException.class, () -> service.changePassword(
                new ChangePasswordCommand(USER_ID, "errada", "novaSenha123", "novaSenha123")));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("recusa quando a confirmação não coincide com a nova senha")
    void recusaConfirmacaoDivergente() {
        given(loadUserPort.findById(USER_ID))
                .willReturn(Optional.of(DomainFixtures.usuarioPersistido(USER_ID)));
        given(passwordEncoderPort.matches(anyString(), anyString())).willReturn(true);

        assertThrows(InvalidPasswordException.class, () -> service.changePassword(
                new ChangePasswordCommand(USER_ID, "senhaAtual", "novaSenha123", "outraCoisa")));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("falha quando o usuário não existe")
    void falhaUsuarioInexistente() {
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.changePassword(
                new ChangePasswordCommand(USER_ID, "senhaAtual", "novaSenha123", "novaSenha123")));
    }
}

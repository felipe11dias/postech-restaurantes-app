package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.command.RequestPasswordResetCommand;
import com.postech.restaurantes.application.port.in.command.ResetPasswordCommand;
import com.postech.restaurantes.application.port.out.LoadPasswordResetTokenPort;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.ResetTokenGeneratorPort;
import com.postech.restaurantes.application.port.out.SavePasswordResetTokenPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.application.port.out.SendPasswordResetMailPort;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.model.PasswordResetToken;
import com.postech.restaurantes.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final long EXPIRACAO_MINUTOS = 30;
    private static final String TOKEN_CRU = "token-cru";
    private static final String TOKEN_HASH = "token-hash";

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveUserPort saveUserPort;
    @Mock
    private LoadPasswordResetTokenPort loadTokenPort;
    @Mock
    private SavePasswordResetTokenPort saveTokenPort;
    @Mock
    private ResetTokenGeneratorPort tokenGeneratorPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @Mock
    private SendPasswordResetMailPort sendMailPort;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(loadUserPort, saveUserPort, loadTokenPort, saveTokenPort,
                tokenGeneratorPort, passwordEncoderPort, sendMailPort, new DirectTransactionPort(),
                EXPIRACAO_MINUTOS);
    }

    @Test
    @DisplayName("emite e envia o token quando o e-mail existe, guardando apenas o hash")
    void emiteTokenParaEmailExistente() {
        User user = DomainFixtures.usuarioPersistido();
        given(loadUserPort.findByEmail("maria@email.com")).willReturn(Optional.of(user));
        given(tokenGeneratorPort.generateToken()).willReturn(TOKEN_CRU);
        given(tokenGeneratorPort.hash(TOKEN_CRU)).willReturn(TOKEN_HASH);

        service.requestReset(new RequestPasswordResetCommand("maria@email.com"));

        ArgumentCaptor<PasswordResetToken> capturado =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(saveTokenPort).save(capturado.capture());
        assertEquals(TOKEN_HASH, capturado.getValue().getTokenHash());
        assertTrue(capturado.getValue().getExpiresAt().isAfter(LocalDateTime.now()));

        // O e-mail recebe o token em claro; o banco, só o hash.
        verify(sendMailPort).sendPasswordResetToken("maria@email.com", TOKEN_CRU);
    }

    @Test
    @DisplayName("normaliza o e-mail antes de procurar o usuário")
    void normalizaEmailNaBusca() {
        given(loadUserPort.findByEmail("maria@email.com")).willReturn(Optional.empty());

        service.requestReset(new RequestPasswordResetCommand("  MARIA@Email.COM "));

        verify(loadUserPort).findByEmail("maria@email.com");
    }

    @Test
    @DisplayName("e-mail inexistente não gera token nem erro — a resposta é idêntica")
    void emailInexistenteNaoGeraToken() {
        given(loadUserPort.findByEmail(anyString())).willReturn(Optional.empty());

        service.requestReset(new RequestPasswordResetCommand("ninguem@email.com"));

        verify(saveTokenPort, never()).save(any());
        verify(sendMailPort, never()).sendPasswordResetToken(anyString(), anyString());
    }

    @Test
    @DisplayName("redefine a senha e queima o token")
    void redefineSenhaComTokenValido() {
        UUID userId = UUID.randomUUID();
        User user = DomainFixtures.usuarioPersistido(userId);
        PasswordResetToken token = PasswordResetToken.issue(
                userId, TOKEN_HASH, LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS));

        given(tokenGeneratorPort.hash(TOKEN_CRU)).willReturn(TOKEN_HASH);
        given(loadTokenPort.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(token));
        given(loadUserPort.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoderPort.encode("novaSenha123")).willReturn("$2a$10$novohash");

        service.resetPassword(new ResetPasswordCommand(TOKEN_CRU, "novaSenha123", "novaSenha123"));

        assertEquals("$2a$10$novohash", user.getPassword());
        assertTrue(token.isUsed());
        verify(saveUserPort).save(user);
        verify(saveTokenPort).save(token);
    }

    @Test
    @DisplayName("recusa token já utilizado")
    void recusaTokenJaUsado() {
        PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), TOKEN_HASH, LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS));
        token.markUsed();

        given(tokenGeneratorPort.hash(TOKEN_CRU)).willReturn(TOKEN_HASH);
        given(loadTokenPort.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(token));

        assertThrows(InvalidOrExpiredTokenException.class, () -> service.resetPassword(
                new ResetPasswordCommand(TOKEN_CRU, "novaSenha123", "novaSenha123")));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("recusa token expirado")
    void recusaTokenExpirado() {
        PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), TOKEN_HASH, LocalDateTime.now().minusMinutes(1));

        given(tokenGeneratorPort.hash(TOKEN_CRU)).willReturn(TOKEN_HASH);
        given(loadTokenPort.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(token));

        assertThrows(InvalidOrExpiredTokenException.class, () -> service.resetPassword(
                new ResetPasswordCommand(TOKEN_CRU, "novaSenha123", "novaSenha123")));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("recusa token inexistente")
    void recusaTokenInexistente() {
        given(tokenGeneratorPort.hash(TOKEN_CRU)).willReturn(TOKEN_HASH);
        given(loadTokenPort.findByTokenHash(TOKEN_HASH)).willReturn(Optional.empty());

        assertThrows(InvalidOrExpiredTokenException.class, () -> service.resetPassword(
                new ResetPasswordCommand(TOKEN_CRU, "novaSenha123", "novaSenha123")));
    }

    @Test
    @DisplayName("recusa confirmação divergente antes mesmo de consultar o token")
    void recusaConfirmacaoDivergente() {
        assertThrows(InvalidPasswordException.class, () -> service.resetPassword(
                new ResetPasswordCommand(TOKEN_CRU, "novaSenha123", "outraCoisa")));

        verify(loadTokenPort, never()).findByTokenHash(anyString());
    }
}

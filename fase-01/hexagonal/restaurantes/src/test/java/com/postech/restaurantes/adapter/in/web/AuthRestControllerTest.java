package com.postech.restaurantes.adapter.in.web;

import com.postech.restaurantes.adapter.in.web.dto.v1.request.ForgotPasswordRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.LoginRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.ResetPasswordRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.AuthResponse;
import com.postech.restaurantes.application.port.in.AuthenticateUseCase;
import com.postech.restaurantes.application.port.in.RequestPasswordResetUseCase;
import com.postech.restaurantes.application.port.in.ResetPasswordUseCase;
import com.postech.restaurantes.application.port.in.command.AuthenticateCommand;
import com.postech.restaurantes.application.port.in.command.RequestPasswordResetCommand;
import com.postech.restaurantes.application.port.in.command.ResetPasswordCommand;
import com.postech.restaurantes.application.port.in.view.AuthView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthRestController — autenticação e recuperação de senha")
class AuthRestControllerTest {

    @Mock
    private AuthenticateUseCase authenticateUseCase;
    @Mock
    private RequestPasswordResetUseCase requestPasswordResetUseCase;
    @Mock
    private ResetPasswordUseCase resetPasswordUseCase;

    @InjectMocks
    private AuthRestController controller;

    @Test
    @DisplayName("login devolve 200 com o token emitido")
    void login() {
        given(authenticateUseCase.authenticate(any(AuthenticateCommand.class)))
                .willReturn(AuthView.bearer("jwt-token", 3_600_000L));

        ResponseEntity<AuthResponse> response =
                controller.login(new LoginRequest("maria.silva", "senha12345"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", response.getBody().token());
        assertEquals("Bearer", response.getBody().type());
        assertEquals(3_600_000L, response.getBody().expiresIn());

        ArgumentCaptor<AuthenticateCommand> captor =
                ArgumentCaptor.forClass(AuthenticateCommand.class);
        verify(authenticateUseCase).authenticate(captor.capture());
        assertEquals("maria.silva", captor.getValue().login());
        assertEquals("senha12345", captor.getValue().rawPassword());
    }

    /**
     * Sempre 202, exista ou não o e-mail: responder 404 para e-mail inexistente
     * transformaria este endpoint público em uma forma de descobrir quais contas
     * existem.
     */
    @Test
    @DisplayName("solicitação de recuperação devolve 202 sem corpo")
    void forgotPassword() {
        ResponseEntity<Void> response =
                controller.forgotPassword(new ForgotPasswordRequest("maria@email.com"));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());

        ArgumentCaptor<RequestPasswordResetCommand> captor =
                ArgumentCaptor.forClass(RequestPasswordResetCommand.class);
        verify(requestPasswordResetUseCase).requestReset(captor.capture());
        assertEquals("maria@email.com", captor.getValue().email());
    }

    @Test
    @DisplayName("redefinição de senha devolve 204 sem corpo")
    void resetPassword() {
        ResponseEntity<Void> response = controller.resetPassword(
                new ResetPasswordRequest("TOKEN-OPACO", "nova12345", "nova12345"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        ArgumentCaptor<ResetPasswordCommand> captor =
                ArgumentCaptor.forClass(ResetPasswordCommand.class);
        verify(resetPasswordUseCase).resetPassword(captor.capture());
        assertEquals("TOKEN-OPACO", captor.getValue().rawToken());
        assertEquals("nova12345", captor.getValue().newPassword());
    }
}

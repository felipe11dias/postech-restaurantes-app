package com.postech.restaurantes.controller;

import com.postech.restaurantes.service.AuthenticationService;
import com.postech.restaurantes.service.PasswordResetService;
import com.postech.restaurantes.vo.v1.request.ForgotPasswordRequest;
import com.postech.restaurantes.vo.v1.request.LoginRequest;
import com.postech.restaurantes.vo.v1.request.ResetPasswordRequest;
import com.postech.restaurantes.vo.v1.response.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — autenticação e recuperação de senha")
class AuthControllerTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private PasswordResetService passwordResetService;

    @InjectMocks private AuthController authController;

    @Test
    @DisplayName("login devolve 200 com o token emitido")
    void login_deveDevolver200ComToken() {
        LoginRequest request = new LoginRequest("joao.silva", "senhaSegura123");
        AuthResponse esperado = AuthResponse.bearer("jwt-token", 3_600_000L);
        when(authenticationService.login(request)).thenReturn(esperado);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(esperado);
    }

    /**
     * A resposta é sempre 202, exista ou não o e-mail: qualquer diferença aqui
     * permitiria descobrir quais contas existem.
     */
    @Test
    @DisplayName("solicitação de recuperação devolve 202 sem corpo")
    void forgotPassword_deveDevolver202() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("joao@email.com");

        ResponseEntity<Void> response = authController.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
        verify(passwordResetService).requestReset(request);
    }

    @Test
    @DisplayName("redefinição de senha devolve 204 sem corpo")
    void resetPassword_deveDevolver204() {
        ResetPasswordRequest request = new ResetPasswordRequest("TOKEN", "nova12345", "nova12345");

        ResponseEntity<Void> response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(passwordResetService).resetPassword(request);
    }
}

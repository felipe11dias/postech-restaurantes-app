package com.postech.restaurantes.adapter.in.web;

import com.postech.restaurantes.adapter.in.web.dto.v1.request.ForgotPasswordRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.LoginRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.request.ResetPasswordRequest;
import com.postech.restaurantes.adapter.in.web.dto.v1.response.AuthResponse;
import com.postech.restaurantes.application.port.in.AuthenticateUseCase;
import com.postech.restaurantes.application.port.in.RequestPasswordResetUseCase;
import com.postech.restaurantes.application.port.in.ResetPasswordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada REST para autenticação e recuperação de senha. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login e recuperação de senha")
public class AuthRestController {

    private final AuthenticateUseCase authenticateUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public AuthRestController(AuthenticateUseCase authenticateUseCase,
                              RequestPasswordResetUseCase requestPasswordResetUseCase,
                              ResetPasswordUseCase resetPasswordUseCase) {
        this.authenticateUseCase = authenticateUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Valida as credenciais e emite um token de acesso")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(AuthResponse.from(authenticateUseCase.authenticate(request.toCommand())));
    }

    /**
     * Sempre responde 202, exista ou não o e-mail informado.
     *
     * <p>Responder 404 para e-mail inexistente transformaria este endpoint público
     * em uma forma de descobrir quais e-mails estão cadastrados. A resposta
     * idêntica nos dois casos é a contrapartida, no adapter, do silêncio que o caso
     * de uso mantém quando não encontra o usuário.</p>
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita o envio de um token de redefinição de senha")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordResetUseCase.requestReset(request.toCommand());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha a partir de um token de recuperação")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.resetPassword(request.toCommand());
        return ResponseEntity.noContent().build();
    }
}

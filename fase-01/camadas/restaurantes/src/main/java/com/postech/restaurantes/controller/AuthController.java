package com.postech.restaurantes.controller;

import com.postech.restaurantes.service.AuthenticationService;
import com.postech.restaurantes.service.PasswordResetService;
import com.postech.restaurantes.vo.v1.request.ForgotPasswordRequest;
import com.postech.restaurantes.vo.v1.request.LoginRequest;
import com.postech.restaurantes.vo.v1.request.ResetPasswordRequest;
import com.postech.restaurantes.vo.v1.response.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticação e recuperação de senha.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationService authenticationService,
                          PasswordResetService passwordResetService) {
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    /**
     * Sempre responde 202, exista ou não o e-mail informado, para não revelar
     * quais e-mails estão cadastrados (evita enumeração de contas).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}

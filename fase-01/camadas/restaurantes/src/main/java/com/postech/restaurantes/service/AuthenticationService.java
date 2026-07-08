package com.postech.restaurantes.service;

import com.postech.restaurantes.security.JwtService;
import com.postech.restaurantes.vo.v1.request.LoginRequest;
import com.postech.restaurantes.vo.v1.response.AuthResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Valida as credenciais (login e senha) e, se válidas, emite um token JWT.
 * A comparação senha-hash é feita pelo AuthenticationManager via BCrypt.
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 UserDetailsService userDetailsService,
                                 JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.login());
        String token = jwtService.generateToken(userDetails);
        return AuthResponse.bearer(token, jwtService.getExpiration());
    }
}

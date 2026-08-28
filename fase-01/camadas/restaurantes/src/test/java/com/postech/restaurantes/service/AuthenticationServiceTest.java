package com.postech.restaurantes.service;

import com.postech.restaurantes.security.JwtService;
import com.postech.restaurantes.vo.v1.request.LoginRequest;
import com.postech.restaurantes.vo.v1.response.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService — validação de credenciais e emissão do token")
class AuthenticationServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthenticationService authenticationService;

    private UserDetails usuario() {
        return User.withUsername("joao.silva").password("HASH").authorities(List.of()).build();
    }

    @Test
    @DisplayName("credenciais válidas devolvem token Bearer com a expiração configurada")
    void login_comCredenciaisValidas_deveEmitirToken() {
        UserDetails userDetails = usuario();
        when(userDetailsService.loadUserByUsername("joao.silva")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtService.getExpiration()).thenReturn(3_600_000L);

        AuthResponse response = authenticationService.login(
                new LoginRequest("joao.silva", "senhaSegura123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3_600_000L);
    }

    @Test
    @DisplayName("delega a conferência de senha ao AuthenticationManager")
    void login_deveDelegarAConferenciaDeSenha() {
        when(userDetailsService.loadUserByUsername("joao.silva")).thenReturn(usuario());

        authenticationService.login(new LoginRequest("joao.silva", "senhaSegura123"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(captor.getValue().getPrincipal()).isEqualTo("joao.silva");
        assertThat(captor.getValue().getCredentials()).isEqualTo("senhaSegura123");
    }

    /** Credenciais recusadas não podem chegar à emissão do token. */
    @Test
    @DisplayName("credenciais inválidas propagam a exceção sem emitir token")
    void login_comCredenciaisInvalidas_naoDeveEmitirToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(
                new LoginRequest("joao.silva", "senha-errada")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService, userDetailsService);
    }
}

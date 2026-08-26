package com.postech.restaurantes.adapter.in.web.security;

import com.postech.restaurantes.application.port.out.TokenVerifierPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adapter de entrada: traduz o cabeçalho {@code Authorization: Bearer ...} em uma
 * autenticação no contexto de segurança do Spring.
 *
 * <p>Depende do {@link TokenVerifierPort}, não do adapter de JWT: este filtro não
 * sabe se o token é um JWT, um PASETO ou uma referência opaca. Sabe apenas pedir
 * a verificação e receber, ou não, uma identidade.</p>
 *
 * <p>Tokens ausentes ou inválidos seguem sem autenticar. Quem decide se a rota
 * exigia autenticação é o {@code SecurityFilterChain}, mais adiante — este filtro
 * apenas estabelece quem é o requisitante, quando dá para saber.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final TokenVerifierPort tokenVerifierPort;

    public JwtAuthenticationFilter(TokenVerifierPort tokenVerifierPort) {
        this.tokenVerifierPort = tokenVerifierPort;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HEADER);
        if (authHeader != null
                && authHeader.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            tokenVerifierPort.verify(authHeader.substring(PREFIX.length()))
                    .ifPresent(principal -> autenticar(principal, request));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(TokenVerifierPort.AuthenticatedPrincipal principal,
                            HttpServletRequest request) {
        var authorities = principal.roles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(
                principal.login(), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

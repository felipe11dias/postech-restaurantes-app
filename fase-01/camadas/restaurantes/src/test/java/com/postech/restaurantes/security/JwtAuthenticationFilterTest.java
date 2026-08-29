package com.postech.restaurantes.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O filtro nunca barra a requisição: token ausente, malformado ou inválido
 * apenas seguem sem autenticar, deixando a decisão de acesso para as regras do
 * SecurityFilterChain. É esse contrato — sempre chamar a cadeia — que estes
 * testes protegem.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter — autenticação a partir do cabeçalho Bearer")
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token-valido";

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails usuario() {
        return User.withUsername("joao.silva").password("HASH")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))).build();
    }

    @Test
    @DisplayName("token válido autentica o usuário no contexto de segurança")
    void comTokenValido_deveAutenticar() throws Exception {
        UserDetails userDetails = usuario();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("joao.silva");
        when(userDetailsService.loadUserByUsername("joao.silva")).thenReturn(userDetails);
        when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
        assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("requisição sem cabeçalho Authorization segue sem autenticar")
    void semCabecalho_deveSeguirSemAutenticar() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("cabeçalho em outro esquema que não Bearer segue sem autenticar")
    void comEsquemaDiferente_deveSeguirSemAutenticar() throws Exception {
        request.addHeader("Authorization", "Basic am9hbzpzZW5oYQ==");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("token cuja validação falha não autentica, mas a requisição segue")
    void comTokenInvalido_deveSeguirSemAutenticar() throws Exception {
        UserDetails userDetails = usuario();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("joao.silva");
        when(userDetailsService.loadUserByUsername("joao.silva")).thenReturn(userDetails);
        when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("token sem subject não autentica")
    void comTokenSemSubject_deveSeguirSemAutenticar() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    /** Contexto já autenticado (outro filtro chegou antes) não é sobrescrito. */
    @Test
    @DisplayName("contexto já autenticado é preservado")
    void comContextoJaAutenticado_devePreservar() throws Exception {
        Authentication existente = new UsernamePasswordAuthenticationToken("outro.usuario", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existente);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("joao.silva");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existente);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("token expirado ou corrompido não interrompe a cadeia de filtros")
    void comTokenQueLancaExcecao_deveSeguirSemAutenticar() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenThrow(new IllegalArgumentException("token corrompido"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("usuário do token que não existe mais não interrompe a cadeia")
    void comUsuarioInexistente_deveSeguirSemAutenticar() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn("fantasma");
        when(userDetailsService.loadUserByUsername("fantasma"))
                .thenThrow(new UsernameNotFoundException("Login não encontrado: fantasma"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

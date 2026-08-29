package com.postech.restaurantes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("JwtService — emissão e validação de token")
class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-no-minimo-256-bits-para-hmac-sha256";
    private static final String OUTRO_SECRET = "outro-segredo-de-teste-com-no-minimo-256-bits-hmac-sha";
    private static final long EXPIRACAO_MS = 3_600_000L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRACAO_MS);

    private UserDetails usuario(String login) {
        return User.withUsername(login).password("irrelevante")
                .authorities(List.of()).build();
    }

    @Test
    @DisplayName("gera token cujo subject é o login do usuário")
    void generateToken_deveUsarOLoginComoSubject() {
        String token = jwtService.generateToken(usuario("joao.silva"));

        assertThat(jwtService.extractUsername(token)).isEqualTo("joao.silva");
    }

    @Test
    @DisplayName("expõe a expiração configurada")
    void getExpiration_deveDevolverOValorConfigurado() {
        assertThat(jwtService.getExpiration()).isEqualTo(EXPIRACAO_MS);
    }

    @Test
    @DisplayName("token recém-emitido é válido para o próprio usuário")
    void isTokenValid_paraOProprioUsuario_deveSerVerdadeiro() {
        String token = jwtService.generateToken(usuario("joao.silva"));

        assertThat(jwtService.isTokenValid(token, usuario("joao.silva"))).isTrue();
    }

    @Test
    @DisplayName("token de um usuário não vale para outro")
    void isTokenValid_paraOutroUsuario_deveSerFalso() {
        String token = jwtService.generateToken(usuario("joao.silva"));

        assertThat(jwtService.isTokenValid(token, usuario("maria.souza"))).isFalse();
    }

    /**
     * A expiração é conferida na validação, e não só na leitura das claims: um
     * token vencido, ainda que corretamente assinado, não pode autenticar.
     */
    @Test
    @DisplayName("token expirado é rejeitado")
    void isTokenValid_comTokenExpirado_deveRejeitar() {
        JwtService expiraImediatamente = new JwtService(SECRET, -1_000L);
        String token = expiraImediatamente.generateToken(usuario("joao.silva"));

        assertThatThrownBy(() -> expiraImediatamente.isTokenValid(token, usuario("joao.silva")))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("token assinado com outro segredo é rejeitado")
    void extractUsername_comAssinaturaDeOutroSegredo_deveLancar() {
        SecretKey outraChave = Keys.hmacShaKeyFor(OUTRO_SECRET.getBytes(StandardCharsets.UTF_8));
        String tokenForjado = Jwts.builder()
                .subject("joao.silva")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRACAO_MS))
                .signWith(outraChave)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername(tokenForjado))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    /**
     * A conferência de expiração feita pelo próprio serviço é uma segunda linha
     * de defesa: na prática o parser do jjwt já barra o token vencido antes dela.
     * Substituir o parser é a única forma de exercitar esse caminho — e é o que
     * garante que um token com validade vencida seja recusado, e não aceito, caso
     * a checagem da biblioteca deixe de acontecer.
     */
    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("token com expiração no passado não é considerado válido")
    void isTokenValid_comExpiracaoNoPassado_deveSerFalso() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("joao.silva");
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().minusSeconds(60)));

        JwtParserBuilder builder = mock(JwtParserBuilder.class);
        JwtParser parser = mock(JwtParser.class);
        Jws<Claims> jws = mock(Jws.class);
        when(builder.verifyWith(any(SecretKey.class))).thenReturn(builder);
        when(builder.build()).thenReturn(parser);
        when(parser.parseSignedClaims("token-vencido")).thenReturn(jws);
        when(jws.getPayload()).thenReturn(claims);

        try (MockedStatic<Jwts> jwts = mockStatic(Jwts.class)) {
            jwts.when(Jwts::parser).thenReturn(builder);

            assertThat(jwtService.isTokenValid("token-vencido", usuario("joao.silva"))).isFalse();
        }
    }

    @Test
    @DisplayName("texto que não é um JWT é rejeitado")
    void extractUsername_comTextoQualquer_deveLancar() {
        assertThatThrownBy(() -> jwtService.extractUsername("isto-nao-e-um-jwt"))
                .isInstanceOf(io.jsonwebtoken.MalformedJwtException.class);
    }
}

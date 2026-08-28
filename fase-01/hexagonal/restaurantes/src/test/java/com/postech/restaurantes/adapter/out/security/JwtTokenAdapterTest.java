package com.postech.restaurantes.adapter.out.security;

import com.postech.restaurantes.application.port.out.TokenVerifierPort.AuthenticatedPrincipal;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toda menção a JWT no sistema começa e termina neste adapter — então é aqui que
 * o formato do token precisa ser fixado. Um token ruim não é caso excepcional: a
 * verificação devolve Optional vazio e a requisição segue sem autenticação.
 */
class JwtTokenAdapterTest {

    private static final String SEGREDO = "segredo-de-teste-com-no-minimo-256-bits-para-hmac-sha256";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-com-256-bits-para-hmac-sha256";
    private static final long EXPIRACAO_MS = 3_600_000L;

    private final JwtTokenAdapter adapter = new JwtTokenAdapter(SEGREDO, EXPIRACAO_MS);

    private SecretKey chave(String segredo) {
        return Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("emissão")
    class Emissao {

        @Test
        @DisplayName("o token identifica o usuário pelo login e carrega os papéis")
        void tokenCarregaLoginEPapeis() {
            User user = DomainFixtures.usuarioPersistido();

            AuthenticatedPrincipal principal = adapter.verify(adapter.generateToken(user)).orElseThrow();

            assertEquals(user.getLogin(), principal.login());
            assertEquals(Set.of("ROLE_CUSTOMER"), principal.roles());
        }

        @Test
        @DisplayName("expõe a expiração configurada")
        void expiracaoConfigurada() {
            assertEquals(EXPIRACAO_MS, adapter.expirationInMillis());
        }
    }

    @Nested
    @DisplayName("verificação")
    class Verificacao {

        @Test
        @DisplayName("token assinado com outro segredo é recusado")
        void assinaturaDeOutroSegredo() {
            String forjado = Jwts.builder()
                    .subject("maria.silva")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRACAO_MS))
                    .signWith(chave(OUTRO_SEGREDO))
                    .compact();

            assertTrue(adapter.verify(forjado).isEmpty());
        }

        @Test
        @DisplayName("token expirado é recusado")
        void tokenExpirado() {
            JwtTokenAdapter jaExpirado = new JwtTokenAdapter(SEGREDO, -1_000L);

            String token = jaExpirado.generateToken(DomainFixtures.usuarioPersistido());

            assertTrue(jaExpirado.verify(token).isEmpty());
        }

        @Test
        @DisplayName("texto que não é um JWT é recusado")
        void textoQualquer() {
            assertTrue(adapter.verify("isto-nao-e-um-jwt").isEmpty());
        }

        @Test
        @DisplayName("token nulo é recusado")
        void tokenNulo() {
            assertTrue(adapter.verify(null).isEmpty());
        }

        /**
         * Um token válido mas sem a claim de papéis autentica sem authorities —
         * o portador é reconhecido, mas não recebe autorização nenhuma.
         */
        @Test
        @DisplayName("token sem a claim de papéis resulta em nenhum papel")
        void semClaimDePapeis() {
            String semPapeis = Jwts.builder()
                    .subject("maria.silva")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRACAO_MS))
                    .signWith(chave(SEGREDO))
                    .compact();

            AuthenticatedPrincipal principal = adapter.verify(semPapeis).orElseThrow();

            assertEquals("maria.silva", principal.login());
            assertTrue(principal.roles().isEmpty());
        }

        @Test
        @DisplayName("claim de papéis que não é lista resulta em nenhum papel")
        void claimDePapeisComTipoInesperado() {
            String claimTorta = Jwts.builder()
                    .subject("maria.silva")
                    .claim("roles", "ROLE_CUSTOMER")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRACAO_MS))
                    .signWith(chave(SEGREDO))
                    .compact();

            assertTrue(adapter.verify(claimTorta).orElseThrow().roles().isEmpty());
        }
    }

    @Nested
    @DisplayName("AuthenticatedPrincipal")
    class Principal {

        @Test
        @DisplayName("expõe login e papéis")
        void exponeOsCampos() {
            AuthenticatedPrincipal principal =
                    new AuthenticatedPrincipal("maria.silva", Set.of("ROLE_CUSTOMER"));

            assertEquals("maria.silva", principal.login());
            assertEquals(Set.of("ROLE_CUSTOMER"), principal.roles());
            assertEquals(principal, new AuthenticatedPrincipal("maria.silva", Set.of("ROLE_CUSTOMER")));
            assertFalse(Optional.of(principal).isEmpty());
        }
    }
}

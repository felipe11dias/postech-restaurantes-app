package com.postech.restaurantes.adapter.out.security;

import com.postech.restaurantes.application.port.out.TokenProviderPort;
import com.postech.restaurantes.application.port.out.TokenVerifierPort;
import com.postech.restaurantes.domain.model.Role;
import com.postech.restaurantes.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter de saída que realiza a emissão ({@link TokenProviderPort}) e a
 * verificação ({@link TokenVerifierPort}) de tokens, usando JWT assinado com
 * HMAC-SHA256.
 *
 * <p>Toda menção a JWT no sistema começa e termina nesta classe. O caso de uso de
 * login sabe apenas que recebeu "um token que dura N milissegundos"; o filtro de
 * autenticação sabe apenas que "este texto corresponde a este login com estes
 * papéis". Trocar para tokens opacos com introspecção seria reescrever este
 * arquivo e nada mais.</p>
 *
 * <p>O segredo e a expiração vêm de variáveis de ambiente; o segredo precisa ter
 * ao menos 256 bits para o algoritmo.</p>
 */
@Component
public class JwtTokenAdapter implements TokenProviderPort, TokenVerifierPort {

    private static final String CLAIM_ROLES = "roles";

    private final String secret;
    private final long expiration;

    public JwtTokenAdapter(@Value("${security.jwt.secret}") String secret,
                           @Value("${security.jwt.expiration}") long expiration) {
        this.secret = secret;
        this.expiration = expiration;
    }

    @Override
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getLogin())
                .claim(CLAIM_ROLES, user.getRoles().stream()
                        .map(Role::getName)
                        .map(Enum::name)
                        .toList())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(signingKey())
                .compact();
    }

    @Override
    public long expirationInMillis() {
        return expiration;
    }

    /**
     * Os papéis viajam dentro do próprio token.
     *
     * <p>Isso evita uma consulta ao banco por requisição só para montar as
     * authorities. O preço é que uma alteração de papéis só passa a valer no
     * próximo login — aceitável no escopo desta fase, e registrado aqui para que a
     * escolha não pareça acidental.</p>
     */
    @Override
    public Optional<AuthenticatedPrincipal> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedPrincipal(claims.getSubject(), rolesFrom(claims)));
        } catch (JwtException | IllegalArgumentException e) {
            // Assinatura inválida, token expirado ou malformado. Não é uma falha do
            // servidor: é um cliente apresentando credencial ruim, e a requisição
            // simplesmente segue sem autenticação.
            return Optional.empty();
        }
    }

    private Set<String> rolesFrom(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        if (!(roles instanceof List<?> list)) {
            return Set.of();
        }
        return list.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

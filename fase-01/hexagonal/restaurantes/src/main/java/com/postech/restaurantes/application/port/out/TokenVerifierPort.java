package com.postech.restaurantes.application.port.out;

import java.util.Optional;
import java.util.Set;

/**
 * Port de saída: verificação de um token de acesso apresentado por um cliente.
 *
 * <p>É a operação simétrica de {@link TokenProviderPort#generateToken}. Está
 * separada em outro port pelo mesmo motivo que os demais: quem emite e quem
 * confere são clientes diferentes — o caso de uso de login emite, o filtro de
 * autenticação confere — e nenhum dos dois precisa do método do outro.</p>
 *
 * <p>Assim como {@link AuditorPort}, este port não é chamado por nenhum caso de
 * uso: quem o consome é o adapter de entrada web. Declará-lo aqui, e não fazer o
 * filtro depender diretamente do adapter de JWT, é o que impede um adapter de
 * entrada de conhecer um adapter de saída — os dois continuam se falando apenas
 * através do vocabulário da aplicação.</p>
 */
public interface TokenVerifierPort {

    /**
     * Devolve a identidade contida no token, ou {@link Optional#empty()} se ele
     * for inválido, expirado ou malformado. A ausência de valor é a resposta
     * normal para um token ruim — não é um caso excepcional, é o dia a dia de uma
     * API pública.
     */
    Optional<AuthenticatedPrincipal> verify(String token);

    /** Identidade extraída de um token válido. */
    record AuthenticatedPrincipal(String login, Set<String> roles) {
    }
}

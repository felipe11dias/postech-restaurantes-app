package com.postech.restaurantes.adapter.in.web.exception;

/**
 * URIs estáveis usadas no campo {@code type} do ProblemDetail (RFC 7807), uma por
 * categoria de erro.
 *
 * <p>Servem para que o cliente da API distinga o tipo de erro de forma
 * programática, em vez de inspecionar {@code title} em texto livre — que é
 * português, pode ser reescrito e não deveria virar contrato acidental.</p>
 */
public final class ProblemType {

    public static final String RECURSO_NAO_ENCONTRADO = "/problemas/recurso-nao-encontrado";
    public static final String CONFLITO_DE_DADOS = "/problemas/conflito-de-dados";
    public static final String SENHA_INVALIDA = "/problemas/senha-invalida";
    public static final String OPERACAO_NAO_PERMITIDA = "/problemas/operacao-nao-permitida";
    public static final String ACESSO_NEGADO = "/problemas/acesso-negado";
    public static final String REQUISICAO_INVALIDA = "/problemas/requisicao-invalida";
    public static final String FALHA_AUTENTICACAO = "/problemas/falha-autenticacao";
    public static final String NAO_AUTENTICADO = "/problemas/nao-autenticado";
    public static final String TOKEN_INVALIDO_OU_EXPIRADO = "/problemas/token-invalido-ou-expirado";
    public static final String ERRO_INTERNO = "/problemas/erro-interno";

    private ProblemType() {
    }
}

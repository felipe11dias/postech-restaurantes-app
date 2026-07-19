package com.postech.restaurantes.exception;

/**
 * URIs estáveis usadas no campo "type" do ProblemDetail (RFC 7807), uma por
 * categoria de erro, para que o consumidor da API possa diferenciar o tipo de
 * erro de forma programática em vez de depender só do "status"/"title" em
 * texto livre.
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

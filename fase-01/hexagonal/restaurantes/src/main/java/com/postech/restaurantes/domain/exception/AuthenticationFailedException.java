package com.postech.restaurantes.domain.exception;

/**
 * Lançada quando as credenciais informadas no login não conferem.
 *
 * <p>A mensagem não diz se o que falhou foi o login ou a senha: responder
 * "login inexistente" transformaria o endpoint de autenticação em um oráculo
 * para descobrir quais contas existem.</p>
 *
 * <p>Note que esta exceção é do <em>domínio</em>, e não a
 * {@code AuthenticationException} do Spring Security: quem decide se as
 * credenciais valem é o caso de uso, não o framework. É o que mantém a
 * autenticação testável sem subir contexto nenhum.</p>
 */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException() {
        super("Login ou senha inválidos");
    }
}

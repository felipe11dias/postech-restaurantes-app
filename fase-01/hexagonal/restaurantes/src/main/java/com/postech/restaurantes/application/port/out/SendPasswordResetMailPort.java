package com.postech.restaurantes.application.port.out;

/**
 * Port de saída: notificação do usuário com o token de redefinição.
 *
 * <p>O nome fala de e-mail porque é o canal que o requisito pede, mas o núcleo
 * não sabe nada de SMTP, cabeçalhos ou corpo da mensagem — só que existe um jeito
 * de fazer o token chegar ao dono da conta.</p>
 *
 * <p><strong>Contrato:</strong> a entrega é <em>best-effort</em>. Uma falha no
 * envio não pode propagar exceção. O motivo é de segurança: o endpoint de
 * solicitação responde igual para e-mail existente e inexistente justamente para
 * não revelar quais contas existem — e se uma falha de SMTP virasse 500 apenas no
 * caminho em que o usuário existe, a diferença de status code recriaria o
 * vazamento que a resposta uniforme evita. Cabe ao adapter registrar a falha e
 * retornar normalmente.</p>
 */
public interface SendPasswordResetMailPort {

    void sendPasswordResetToken(String to, String rawToken);
}

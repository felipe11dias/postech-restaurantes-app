package com.postech.restaurantes.adapter.out.mail;

import com.postech.restaurantes.application.port.out.SendPasswordResetMailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adapter de saída que realiza o {@link SendPasswordResetMailPort} via SMTP.
 *
 * <p>Não há frontend nesta fase, então o corpo do e-mail traz o token em claro e a
 * instrução de como usá-lo na API. O token também é registrado em log de nível
 * INFO: como a API nunca o devolve em resposta alguma, este é o único jeito de
 * recuperá-lo para os testes manuais e da coleção Postman.</p>
 */
@Component
public class SmtpPasswordResetMailAdapter implements SendPasswordResetMailPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetMailAdapter.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordResetMailAdapter(JavaMailSender mailSender,
                                        @Value("${mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendPasswordResetToken(String to, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Redefinição de senha - Restaurantes");
        message.setText("""
                Recebemos uma solicitação de redefinição de senha para esta conta.

                Use o token abaixo em POST /api/v1/auth/reset-password (campo "token") \
                para definir uma nova senha:

                %s

                Se você não solicitou essa alteração, ignore este e-mail.
                """.formatted(rawToken));

        // O token é registrado antes da tentativa de envio: se o SMTP estiver fora,
        // o log continua sendo o caminho para recuperá-lo em testes.
        log.info("Token de redefinição de senha gerado para {} (token: {})", to, rawToken);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Contrato do port: a falha de entrega não sobe. Deixar a exceção
            // propagar faria a requisição responder 500 quando o e-mail existe e
            // 202 quando não existe — e essa diferença de status code é, por si só,
            // um jeito de descobrir quais contas estão cadastradas.
            log.error("Falha ao enviar o e-mail de redefinição de senha para {}", to, e);
        }
    }
}

package com.postech.restaurantes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envia o e-mail de redefinição de senha via JavaMailSender (SMTP configurado
 * em application.yml / variáveis MAIL_*). Não há frontend nesta etapa, então o
 * corpo do e-mail traz o token bruto e a instrução de como usá-lo na API.
 *
 * O token também é logado em INFO: é o único jeito de recuperá-lo para testes
 * manuais/automatizados, já que a API nunca o retorna em nenhuma resposta.
 */
@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String from;

    public MailServiceImpl(JavaMailSender mailSender, @Value("${mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendPasswordResetEmail(String to, String rawToken) {
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

        mailSender.send(message);
        log.info("E-mail de redefinição de senha enviado para {} (token: {})", to, rawToken);
    }
}

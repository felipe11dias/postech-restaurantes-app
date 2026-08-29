package com.postech.restaurantes.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailServiceImpl — envio do e-mail de redefinição de senha")
class MailServiceImplTest {

    private static final String REMETENTE = "no-reply@restaurantes.postech";

    @Mock private JavaMailSender mailSender;

    @Test
    @DisplayName("envia a mensagem com remetente, destinatário, assunto e o token no corpo")
    void sendPasswordResetEmail_deveMontarEEnviarAMensagem() {
        MailServiceImpl mailService = new MailServiceImpl(mailSender, REMETENTE);

        mailService.sendPasswordResetEmail("joao@email.com", "TOKEN-OPACO");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo(REMETENTE);
        assertThat(message.getTo()).containsExactly("joao@email.com");
        assertThat(message.getSubject()).isEqualTo("Redefinição de senha - Restaurantes");
        assertThat(message.getText())
                .contains("TOKEN-OPACO")
                .contains("POST /api/v1/auth/reset-password");
    }
}

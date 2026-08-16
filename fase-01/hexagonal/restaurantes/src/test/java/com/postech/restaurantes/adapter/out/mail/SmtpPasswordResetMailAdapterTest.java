package com.postech.restaurantes.adapter.out.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * Testes do adapter de e-mail.
 *
 * <p>O caso interessante aqui é o da falha, e ele é uma regra de segurança, não um
 * detalhe de robustez: se uma queda do SMTP virasse exceção, o endpoint de
 * solicitação responderia 500 para e-mail cadastrado e 202 para e-mail
 * inexistente — e a diferença de status revelaria quais contas existem, anulando a
 * resposta uniforme que o fluxo mantém de propósito.</p>
 */
@ExtendWith(MockitoExtension.class)
class SmtpPasswordResetMailAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("envia o e-mail com o token para o destinatário")
    void enviaEmail() {
        new SmtpPasswordResetMailAdapter(mailSender, "no-reply@teste")
                .sendPasswordResetToken("maria@email.com", "token-cru");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("falha de SMTP não propaga — a resposta não pode revelar se o e-mail existe")
    void falhaDeEnvioNaoPropaga() {
        willThrow(new MailSendException("SMTP fora do ar"))
                .given(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> new SmtpPasswordResetMailAdapter(mailSender, "no-reply@teste")
                .sendPasswordResetToken("maria@email.com", "token-cru"));
    }
}

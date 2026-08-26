package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.RequestPasswordResetUseCase;
import com.postech.restaurantes.application.port.in.ResetPasswordUseCase;
import com.postech.restaurantes.application.port.in.command.RequestPasswordResetCommand;
import com.postech.restaurantes.application.port.in.command.ResetPasswordCommand;
import com.postech.restaurantes.application.port.out.LoadPasswordResetTokenPort;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.ResetTokenGeneratorPort;
import com.postech.restaurantes.application.port.out.SavePasswordResetTokenPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.application.port.out.SendPasswordResetMailPort;
import com.postech.restaurantes.application.port.out.TransactionPort;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.model.PasswordResetToken;
import com.postech.restaurantes.domain.model.User;
import com.postech.restaurantes.domain.model.shared.Email;

import java.time.LocalDateTime;

/**
 * Recuperação de senha sem exigir a senha anterior: solicitação por e-mail (que
 * gera um token de uso único) e redefinição a partir desse token.
 *
 * <p>Implementa os dois input ports porque são as duas metades de um mesmo fluxo e
 * compartilham o entendimento de como o token é emitido e gasto. São interfaces
 * separadas mesmo assim: quem chama a solicitação não fica acoplado à redefinição.</p>
 */
public class PasswordResetService implements RequestPasswordResetUseCase, ResetPasswordUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadPasswordResetTokenPort loadTokenPort;
    private final SavePasswordResetTokenPort saveTokenPort;
    private final ResetTokenGeneratorPort tokenGeneratorPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SendPasswordResetMailPort sendMailPort;
    private final TransactionPort transactionPort;
    private final long tokenExpirationMinutes;

    public PasswordResetService(LoadUserPort loadUserPort,
                                SaveUserPort saveUserPort,
                                LoadPasswordResetTokenPort loadTokenPort,
                                SavePasswordResetTokenPort saveTokenPort,
                                ResetTokenGeneratorPort tokenGeneratorPort,
                                PasswordEncoderPort passwordEncoderPort,
                                SendPasswordResetMailPort sendMailPort,
                                TransactionPort transactionPort,
                                long tokenExpirationMinutes) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loadTokenPort = loadTokenPort;
        this.saveTokenPort = saveTokenPort;
        this.tokenGeneratorPort = tokenGeneratorPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.sendMailPort = sendMailPort;
        this.transactionPort = transactionPort;
        this.tokenExpirationMinutes = tokenExpirationMinutes;
    }

    /**
     * Se o e-mail existir, emite um token e o envia. Se não existir, não faz nada —
     * e o adapter responde exatamente a mesma coisa nos dois casos, para que a API
     * não sirva para descobrir quais e-mails estão cadastrados.
     */
    @Override
    public void requestReset(RequestPasswordResetCommand command) {
        // Normaliza pelo mesmo VO usado no cadastro, senão "JOAO@email.com" não
        // encontraria o registro gravado como "joao@email.com".
        String email = new Email(command.email()).value();
        loadUserPort.findByEmail(email).ifPresent(this::issueToken);
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        if (!command.newPassword().equals(command.confirmPassword())) {
            throw InvalidPasswordException.confirmationMismatch();
        }

        // Gravar a senha nova e queimar o token formam uma unidade: se a segunda
        // falhasse sozinha, o token continuaria valendo para uma nova troca.
        transactionPort.inTransaction(() -> {
            String tokenHash = tokenGeneratorPort.hash(command.rawToken());
            PasswordResetToken resetToken = loadTokenPort.findByTokenHash(tokenHash)
                    .orElseThrow(InvalidOrExpiredTokenException::new);

            if (!resetToken.isUsable()) {
                throw new InvalidOrExpiredTokenException();
            }

            User user = loadUserPort.findById(resetToken.getUserId())
                    .orElseThrow(InvalidOrExpiredTokenException::new);

            user.changePassword(passwordEncoderPort.encode(command.newPassword()));
            saveUserPort.save(user);

            resetToken.markUsed();
            saveTokenPort.save(resetToken);
        });
    }

    private void issueToken(User user) {
        String rawToken = tokenGeneratorPort.generateToken();

        transactionPort.inTransaction(() -> saveTokenPort.save(PasswordResetToken.issue(
                user.getId(),
                tokenGeneratorPort.hash(rawToken),
                LocalDateTime.now().plusMinutes(tokenExpirationMinutes))));

        sendMailPort.sendPasswordResetToken(user.getEmail().value(), rawToken);
    }
}

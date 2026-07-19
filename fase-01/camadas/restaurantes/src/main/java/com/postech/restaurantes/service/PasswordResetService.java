package com.postech.restaurantes.service;

import com.postech.restaurantes.entity.PasswordResetToken;
import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.exception.InvalidPasswordException;
import com.postech.restaurantes.repository.PasswordResetTokenRepository;
import com.postech.restaurantes.repository.UserRepository;
import com.postech.restaurantes.security.PasswordResetTokenGenerator;
import com.postech.restaurantes.vo.v1.request.ForgotPasswordRequest;
import com.postech.restaurantes.vo.v1.request.ResetPasswordRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Fluxo de recuperação de senha sem exigir a senha anterior: solicitação por
 * e-mail (gera um token de uso único) e redefinição a partir desse token.
 */
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final long tokenExpirationMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordResetTokenGenerator tokenGenerator,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                @Value("${mail.reset-token-expiration-minutes}") long tokenExpirationMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.tokenExpirationMinutes = tokenExpirationMinutes;
    }

    /**
     * Se o e-mail existir, gera um token e envia por e-mail. Se não existir,
     * não faz nada — a resposta é idêntica em ambos os casos para não revelar
     * quais e-mails estão cadastrados (evita enumeração de contas).
     */
    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(this::issueToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new InvalidPasswordException("A confirmação não coincide com a nova senha");
        }

        String tokenHash = tokenGenerator.hash(request.token());
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Token inválido ou expirado"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new InvalidOrExpiredTokenException("Token inválido ou expirado");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void issueToken(User user) {
        String rawToken = tokenGenerator.generateToken();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenGenerator.hash(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes))
                .build();
        tokenRepository.save(resetToken);

        mailService.sendPasswordResetEmail(user.getEmail(), rawToken);
    }
}

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService — recuperação de senha por token")
class PasswordResetServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final long EXPIRACAO_MINUTOS = 30L;

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordResetTokenGenerator tokenGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailService mailService;

    private PasswordResetService passwordResetService;

    private PasswordResetService service() {
        if (passwordResetService == null) {
            passwordResetService = new PasswordResetService(userRepository, tokenRepository,
                    tokenGenerator, passwordEncoder, mailService, EXPIRACAO_MINUTOS);
        }
        return passwordResetService;
    }

    private User usuario() {
        return User.builder()
                .id(USER_ID)
                .name("João Silva")
                .email("joao@email.com")
                .login("joao.silva")
                .password("HASH_ANTIGO")
                .build();
    }

    @Nested
    @DisplayName("solicitação de redefinição")
    class Solicitacao {

        @Test
        @DisplayName("e-mail cadastrado gera token de uso único e dispara o e-mail")
        void comEmailCadastrado_deveGerarTokenEEnviarEmail() {
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario()));
            when(tokenGenerator.generateToken()).thenReturn("TOKEN-BRUTO");
            when(tokenGenerator.hash("TOKEN-BRUTO")).thenReturn("HASH-DO-TOKEN");

            service().requestReset(new ForgotPasswordRequest("joao@email.com"));

            ArgumentCaptor<PasswordResetToken> captor =
                    ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());

            PasswordResetToken token = captor.getValue();
            assertThat(token.getUserId()).isEqualTo(USER_ID);
            assertThat(token.getTokenHash()).isEqualTo("HASH-DO-TOKEN");
            assertThat(token.isUsed()).isFalse();
            assertThat(token.getExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS - 1))
                    .isBefore(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS + 1));

            verify(mailService).sendPasswordResetEmail("joao@email.com", "TOKEN-BRUTO");
        }

        /** O token em claro nunca é persistido — só o seu hash. */
        @Test
        @DisplayName("persiste apenas o hash do token, nunca o token em claro")
        void naoDevePersistirOTokenEmClaro() {
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario()));
            when(tokenGenerator.generateToken()).thenReturn("TOKEN-BRUTO");
            when(tokenGenerator.hash("TOKEN-BRUTO")).thenReturn("HASH-DO-TOKEN");

            service().requestReset(new ForgotPasswordRequest("joao@email.com"));

            ArgumentCaptor<PasswordResetToken> captor =
                    ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isNotEqualTo("TOKEN-BRUTO");
        }

        /**
         * Responder igual para e-mail existente e inexistente é o que impede
         * descobrir quais contas existem a partir deste endpoint.
         */
        @Test
        @DisplayName("e-mail não cadastrado não gera token nem e-mail, e não falha")
        void comEmailDesconhecido_naoDeveFazerNada() {
            when(userRepository.findByEmail("fantasma@email.com")).thenReturn(Optional.empty());

            service().requestReset(new ForgotPasswordRequest("fantasma@email.com"));

            verifyNoInteractions(tokenRepository, tokenGenerator, mailService);
        }
    }

    @Nested
    @DisplayName("redefinição a partir do token")
    class Redefinicao {

        private PasswordResetToken tokenValido() {
            return PasswordResetToken.builder()
                    .id(UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc"))
                    .userId(USER_ID)
                    .tokenHash("HASH-DO-TOKEN")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();
        }

        @Test
        @DisplayName("token válido troca a senha e marca o token como usado")
        void comTokenValido_deveTrocarASenha() {
            User user = usuario();
            when(tokenGenerator.hash("TOKEN-BRUTO")).thenReturn("HASH-DO-TOKEN");
            when(tokenRepository.findByTokenHash("HASH-DO-TOKEN")).thenReturn(Optional.of(tokenValido()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("nova12345")).thenReturn("HASH_NOVO");

            service().resetPassword(new ResetPasswordRequest("TOKEN-BRUTO", "nova12345", "nova12345"));

            assertThat(user.getPassword()).isEqualTo("HASH_NOVO");
            verify(userRepository).save(user);

            ArgumentCaptor<PasswordResetToken> captor =
                    ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().isUsed()).isTrue();
        }

        @Test
        @DisplayName("confirmação divergente é recusada antes de olhar o token")
        void comConfirmacaoDivergente_deveLancar() {
            assertThatThrownBy(() -> service().resetPassword(
                    new ResetPasswordRequest("TOKEN-BRUTO", "nova12345", "diferente999")))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("confirmação");

            verifyNoInteractions(tokenRepository, tokenGenerator, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("token inexistente é recusado")
        void comTokenInexistente_deveLancar() {
            when(tokenGenerator.hash("TOKEN-BRUTO")).thenReturn("HASH-DO-TOKEN");
            when(tokenRepository.findByTokenHash("HASH-DO-TOKEN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().resetPassword(
                    new ResetPasswordRequest("TOKEN-BRUTO", "nova12345", "nova12345")))
                    .isInstanceOf(InvalidOrExpiredTokenException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("token já utilizado é recusado")
        void comTokenJaUsado_deveLancar() {
            PasswordResetToken token = tokenValido();
            token.setUsed(true);
            when(tokenGenerator.hash("TOKEN-BRUTO")).thenReturn("HASH-DO-TOKEN");
            when(tokenRepository.findByTokenHash("HASH-DO-TOKEN")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service().resetPassword(
                    new ResetPasswordRequest("TOKEN-BRUTO", "nova12345", "nova12345")))
                    .isInstanceOf(InvalidOrExpiredTokenException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("token expirado é recusado")
        void comTokenExpirado_deveLancar() {
            PasswordResetToken token = tokenValido();
            token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(tokenGenerator.hash("TOKEN-BRUTO")).thenReturn("HASH-DO-TOKEN");
            when(tokenRepository.findByTokenHash("HASH-DO-TOKEN")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service().resetPassword(
                    new ResetPasswordRequest("TOKEN-BRUTO", "nova12345", "nova12345")))
                    .isInstanceOf(InvalidOrExpiredTokenException.class);

            verify(userRepository, never()).save(any());
        }

        /** Token órfão (usuário excluído) responde como token inválido, não como 404. */
        @Test
        @DisplayName("token de usuário que não existe mais é recusado como token inválido")
        void comUsuarioInexistente_deveLancarTokenInvalido() {
            when(tokenGenerator.hash("TOKEN-BRUTO")).thenReturn("HASH-DO-TOKEN");
            when(tokenRepository.findByTokenHash("HASH-DO-TOKEN")).thenReturn(Optional.of(tokenValido()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().resetPassword(
                    new ResetPasswordRequest("TOKEN-BRUTO", "nova12345", "nova12345")))
                    .isInstanceOf(InvalidOrExpiredTokenException.class);

            verify(userRepository, never()).save(any());
        }
    }
}

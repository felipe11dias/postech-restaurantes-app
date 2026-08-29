package com.postech.restaurantes.config;

import com.postech.restaurantes.application.port.in.RequestPasswordResetUseCase;
import com.postech.restaurantes.application.port.in.ResetPasswordUseCase;
import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.DeleteUserPort;
import com.postech.restaurantes.application.port.out.LoadPasswordResetTokenPort;
import com.postech.restaurantes.application.port.out.LoadRolePort;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.ResetTokenGeneratorPort;
import com.postech.restaurantes.application.port.out.SavePasswordResetTokenPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.application.port.out.SendPasswordResetMailPort;
import com.postech.restaurantes.application.port.out.TokenProviderPort;
import com.postech.restaurantes.application.port.out.TransactionPort;
import com.postech.restaurantes.application.service.AuthenticateUserService;
import com.postech.restaurantes.application.service.ChangePasswordService;
import com.postech.restaurantes.application.service.DeleteUserService;
import com.postech.restaurantes.application.service.FindUserService;
import com.postech.restaurantes.application.service.PasswordResetService;
import com.postech.restaurantes.application.service.RegisterUserService;
import com.postech.restaurantes.application.service.UpdateUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Raiz de composição do hexágono. O que este teste protege não é o Spring, e sim
 * a propriedade que a classe existe para preservar: cada caso de uso é montado a
 * partir de <em>ports</em>, sem que nenhuma classe de {@code domain} ou
 * {@code application} precise de anotação de framework. Por isso a fiação é
 * exercitada com dublês dos ports, fora de qualquer contexto Spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UseCaseConfiguration — fiação dos casos de uso")
class UseCaseConfigurationTest {

    @Mock private LoadRolePort loadRolePort;
    @Mock private SaveUserPort saveUserPort;
    @Mock private CheckUserExistsPort checkUserExistsPort;
    @Mock private PasswordEncoderPort passwordEncoderPort;
    @Mock private TransactionPort transactionPort;
    @Mock private LoadUserPort loadUserPort;
    @Mock private DeleteUserPort deleteUserPort;
    @Mock private TokenProviderPort tokenProviderPort;
    @Mock private ResetTokenGeneratorPort resetTokenGeneratorPort;
    @Mock private LoadPasswordResetTokenPort loadPasswordResetTokenPort;
    @Mock private SavePasswordResetTokenPort savePasswordResetTokenPort;
    @Mock private SendPasswordResetMailPort sendPasswordResetMailPort;

    private final UseCaseConfiguration configuration = new UseCaseConfiguration();

    @Test
    @DisplayName("o cadastro é montado sobre os ports de papel, gravação, checagem e senha")
    void registerUserUseCase() {
        assertInstanceOf(RegisterUserService.class, configuration.registerUserUseCase(
                loadRolePort, saveUserPort, checkUserExistsPort, passwordEncoderPort, transactionPort));
    }

    @Test
    @DisplayName("a atualização é montada sobre os ports de leitura, gravação e checagem")
    void updateUserUseCase() {
        assertInstanceOf(UpdateUserService.class, configuration.updateUserUseCase(
                loadUserPort, saveUserPort, checkUserExistsPort, transactionPort));
    }

    @Test
    @DisplayName("a troca de senha é montada sobre os ports de leitura, gravação e hash")
    void changePasswordUseCase() {
        assertInstanceOf(ChangePasswordService.class, configuration.changePasswordUseCase(
                loadUserPort, saveUserPort, passwordEncoderPort, transactionPort));
    }

    @Test
    @DisplayName("a exclusão é montada sobre os ports de checagem e remoção")
    void deleteUserUseCase() {
        assertInstanceOf(DeleteUserService.class, configuration.deleteUserUseCase(
                checkUserExistsPort, deleteUserPort, transactionPort));
    }

    @Test
    @DisplayName("a consulta é montada apenas sobre o port de leitura")
    void findUserUseCase() {
        assertInstanceOf(FindUserService.class, configuration.findUserUseCase(loadUserPort));
    }

    @Test
    @DisplayName("a autenticação é montada sobre os ports de leitura, hash e emissão de token")
    void authenticateUseCase() {
        assertInstanceOf(AuthenticateUserService.class, configuration.authenticateUseCase(
                loadUserPort, passwordEncoderPort, tokenProviderPort));
    }

    /**
     * Um único serviço realiza os dois casos de uso do fluxo de recuperação. Quem
     * injeta continua declarando apenas o port de que precisa, sem ficar sabendo
     * que o outro existe.
     */
    @Test
    @DisplayName("a recuperação de senha atende aos dois casos de uso do fluxo")
    void passwordResetUseCases() {
        PasswordResetService service = configuration.passwordResetService(
                loadUserPort, saveUserPort, loadPasswordResetTokenPort, savePasswordResetTokenPort,
                resetTokenGeneratorPort, passwordEncoderPort, sendPasswordResetMailPort,
                transactionPort, 30L);

        assertInstanceOf(RequestPasswordResetUseCase.class, service);
        assertInstanceOf(ResetPasswordUseCase.class, service);
    }
}

package com.postech.restaurantes.config;

import com.postech.restaurantes.application.port.in.AuthenticateUseCase;
import com.postech.restaurantes.application.port.in.ChangePasswordUseCase;
import com.postech.restaurantes.application.port.in.DeleteUserUseCase;
import com.postech.restaurantes.application.port.in.FindUserUseCase;
import com.postech.restaurantes.application.port.in.RegisterUserUseCase;
import com.postech.restaurantes.application.port.in.RequestPasswordResetUseCase;
import com.postech.restaurantes.application.port.in.ResetPasswordUseCase;
import com.postech.restaurantes.application.port.in.UpdateUserUseCase;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Raiz de composição do hexágono: é aqui que os casos de uso viram beans.
 *
 * <p>Esta classe existe para que <strong>nenhuma</strong> classe de
 * {@code domain} ou {@code application} precise de uma anotação do Spring. A
 * alternativa óbvia — pôr {@code @Service} em cada serviço de aplicação — teria
 * custado uma linha por arquivo e funcionaria igualmente bem em produção; o que
 * ela custaria de verdade é a propriedade que o teste de ArchUnit protege: a de
 * que o núcleo compila e roda sem framework algum no classpath.</p>
 *
 * <p>Concentrar a fiação aqui também torna a arquitetura legível de uma sentada:
 * este arquivo é o mapa de quais portas cada caso de uso consome. Em troca, ele
 * precisa ser editado sempre que um caso de uso ganha uma dependência nova — um
 * incômodo real, e o preço consciente da pureza do núcleo.</p>
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    RegisterUserUseCase registerUserUseCase(LoadRolePort loadRolePort,
                                            SaveUserPort saveUserPort,
                                            CheckUserExistsPort checkUserExistsPort,
                                            PasswordEncoderPort passwordEncoderPort,
                                            TransactionPort transactionPort) {
        return new RegisterUserService(loadRolePort, saveUserPort, checkUserExistsPort,
                passwordEncoderPort, transactionPort);
    }

    @Bean
    UpdateUserUseCase updateUserUseCase(LoadUserPort loadUserPort,
                                        SaveUserPort saveUserPort,
                                        CheckUserExistsPort checkUserExistsPort,
                                        TransactionPort transactionPort) {
        return new UpdateUserService(loadUserPort, saveUserPort, checkUserExistsPort, transactionPort);
    }

    @Bean
    ChangePasswordUseCase changePasswordUseCase(LoadUserPort loadUserPort,
                                                SaveUserPort saveUserPort,
                                                PasswordEncoderPort passwordEncoderPort,
                                                TransactionPort transactionPort) {
        return new ChangePasswordService(loadUserPort, saveUserPort, passwordEncoderPort, transactionPort);
    }

    @Bean
    DeleteUserUseCase deleteUserUseCase(CheckUserExistsPort checkUserExistsPort,
                                        DeleteUserPort deleteUserPort,
                                        TransactionPort transactionPort) {
        return new DeleteUserService(checkUserExistsPort, deleteUserPort, transactionPort);
    }

    @Bean
    FindUserUseCase findUserUseCase(LoadUserPort loadUserPort) {
        return new FindUserService(loadUserPort);
    }

    @Bean
    AuthenticateUseCase authenticateUseCase(LoadUserPort loadUserPort,
                                            PasswordEncoderPort passwordEncoderPort,
                                            TokenProviderPort tokenProviderPort) {
        return new AuthenticateUserService(loadUserPort, passwordEncoderPort, tokenProviderPort);
    }

    /**
     * Um único serviço realiza os dois casos de uso de recuperação de senha.
     *
     * <p>Um bean só, portanto — o Spring o injeta tanto onde se pede
     * {@link RequestPasswordResetUseCase} quanto onde se pede
     * {@link ResetPasswordUseCase}, porque a classe honra as duas interfaces. Quem
     * injeta continua declarando apenas o port de que precisa e não fica sabendo
     * que o outro existe.</p>
     */
    @Bean
    PasswordResetService passwordResetService(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            LoadPasswordResetTokenPort loadTokenPort,
            SavePasswordResetTokenPort saveTokenPort,
            ResetTokenGeneratorPort tokenGeneratorPort,
            PasswordEncoderPort passwordEncoderPort,
            SendPasswordResetMailPort sendMailPort,
            TransactionPort transactionPort,
            @Value("${mail.reset-token-expiration-minutes}") long tokenExpirationMinutes) {
        return new PasswordResetService(loadUserPort, saveUserPort, loadTokenPort, saveTokenPort,
                tokenGeneratorPort, passwordEncoderPort, sendMailPort, transactionPort,
                tokenExpirationMinutes);
    }
}

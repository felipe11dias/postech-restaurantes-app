package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.ChangePasswordUseCase;
import com.postech.restaurantes.application.port.in.command.ChangePasswordCommand;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.application.port.out.TransactionPort;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.User;

/**
 * Troca de senha por quem sabe a senha atual.
 *
 * <p>O serviço confere as duas condições (senha atual correta e confirmação
 * coincidente) e delega ao domínio apenas a troca em si. Conferir a senha atual é
 * regra do caso de uso porque exige comparar contra um hash — algo que só o
 * adapter de segurança sabe fazer; a entidade recebe o hash novo já pronto.</p>
 */
public class ChangePasswordService implements ChangePasswordUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TransactionPort transactionPort;

    public ChangePasswordService(LoadUserPort loadUserPort,
                                 SaveUserPort saveUserPort,
                                 PasswordEncoderPort passwordEncoderPort,
                                 TransactionPort transactionPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.transactionPort = transactionPort;
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        transactionPort.inTransaction(() -> {
            User user = loadUserPort.findById(command.userId())
                    .orElseThrow(() -> ResourceNotFoundException.user(command.userId()));

            if (!passwordEncoderPort.matches(command.currentPassword(), user.getPassword())) {
                throw InvalidPasswordException.currentPasswordMismatch();
            }
            if (!command.newPassword().equals(command.confirmPassword())) {
                throw InvalidPasswordException.confirmationMismatch();
            }

            user.changePassword(passwordEncoderPort.encode(command.newPassword()));
            saveUserPort.save(user);
        });
    }
}

package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.UpdateUserUseCase;
import com.postech.restaurantes.application.port.in.command.UpdateUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.application.port.out.TransactionPort;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.User;
import com.postech.restaurantes.domain.model.shared.Email;

/** Atualização dos dados cadastrais do usuário — nunca da senha. */
public class UpdateUserService implements UpdateUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final CheckUserExistsPort checkUserExistsPort;
    private final TransactionPort transactionPort;

    public UpdateUserService(LoadUserPort loadUserPort,
                             SaveUserPort saveUserPort,
                             CheckUserExistsPort checkUserExistsPort,
                             TransactionPort transactionPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.checkUserExistsPort = checkUserExistsPort;
        this.transactionPort = transactionPort;
    }

    @Override
    public UserView update(UpdateUserCommand command) {
        return transactionPort.inTransaction(() -> {
            User user = loadUserPort.findById(command.userId())
                    .orElseThrow(() -> ResourceNotFoundException.user(command.userId()));

            Email email = new Email(command.email());

            // O próprio usuário é excluído da checagem: sem isso, salvar o cadastro
            // sem trocar o e-mail acusaria conflito consigo mesmo.
            if (checkUserExistsPort.existsByEmailExcluding(email.value(), user.getId())) {
                throw DuplicateResourceException.email(email.value());
            }
            if (checkUserExistsPort.existsByLoginExcluding(command.login(), user.getId())) {
                throw DuplicateResourceException.login(command.login());
            }

            user.updateProfile(command.name(), email, command.login());
            user.replaceAddresses(AddressFactory.toDomain(command.addresses()));

            return UserView.from(saveUserPort.save(user));
        });
    }
}

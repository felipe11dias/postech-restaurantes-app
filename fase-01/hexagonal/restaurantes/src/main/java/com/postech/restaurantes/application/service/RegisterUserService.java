package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.RegisterUserUseCase;
import com.postech.restaurantes.application.port.in.command.RegisterUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.LoadRolePort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.application.port.out.TransactionPort;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.Role;
import com.postech.restaurantes.domain.model.RoleName;
import com.postech.restaurantes.domain.model.User;
import com.postech.restaurantes.domain.model.shared.Email;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Cadastro de usuário.
 *
 * <p>Repare que a classe não tem uma única anotação: nem {@code @Service}, nem
 * {@code @Transactional}. Ela é instanciada por um {@code @Bean} na configuração de
 * bootstrap, fora do hexágono. O ganho não é estético — é que este arquivo compila
 * e roda sem Spring no classpath, o que torna o teste do caso de uso um teste de
 * unidade de verdade, em milissegundos, sem contexto para subir.</p>
 */
public class RegisterUserService implements RegisterUserUseCase {

    private final LoadRolePort loadRolePort;
    private final SaveUserPort saveUserPort;
    private final CheckUserExistsPort checkUserExistsPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TransactionPort transactionPort;

    public RegisterUserService(LoadRolePort loadRolePort,
                               SaveUserPort saveUserPort,
                               CheckUserExistsPort checkUserExistsPort,
                               PasswordEncoderPort passwordEncoderPort,
                               TransactionPort transactionPort) {
        this.loadRolePort = loadRolePort;
        this.saveUserPort = saveUserPort;
        this.checkUserExistsPort = checkUserExistsPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.transactionPort = transactionPort;
    }

    @Override
    public UserView register(RegisterUserCommand command) {
        ensureSelfRegistrationRolesAllowed(command.roles());

        // O VO normaliza para minúsculas ANTES da checagem de duplicidade — é o que
        // faz "Joao@Email.com" colidir com um "joao@email.com" já cadastrado.
        Email email = new Email(command.email());
        ensureEmailIsAvailable(email.value());
        ensureLoginIsAvailable(command.login());

        Set<Role> roles = resolveRoles(command.roles());
        String passwordHash = passwordEncoderPort.encode(command.rawPassword());

        User user = User.newUser(
                command.name(),
                email,
                command.login(),
                passwordHash,
                roles,
                AddressFactory.toDomain(command.addresses()));

        return transactionPort.inTransaction(() -> UserView.from(saveUserPort.save(user)));
    }

    /**
     * O cadastro é um endpoint público (autocadastro). Papéis privilegiados não
     * podem ser atribuídos por aqui — sem esta guarda, qualquer pessoa se
     * registraria como administrador. ROLE_ADMIN só vem de seed ou de um fluxo
     * administrativo autenticado.
     */
    private void ensureSelfRegistrationRolesAllowed(Set<RoleName> names) {
        if (names != null && names.contains(RoleName.ROLE_ADMIN)) {
            throw ForbiddenOperationException.selfRegistrationWithRole(RoleName.ROLE_ADMIN);
        }
    }

    private void ensureEmailIsAvailable(String email) {
        if (checkUserExistsPort.existsByEmailExcluding(email, null)) {
            throw DuplicateResourceException.email(email);
        }
    }

    private void ensureLoginIsAvailable(String login) {
        if (checkUserExistsPort.existsByLoginExcluding(login, null)) {
            throw DuplicateResourceException.login(login);
        }
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um papel");
        }
        return names.stream()
                .map(name -> loadRolePort.findByName(name)
                        .orElseThrow(() -> ResourceNotFoundException.role(name)))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}

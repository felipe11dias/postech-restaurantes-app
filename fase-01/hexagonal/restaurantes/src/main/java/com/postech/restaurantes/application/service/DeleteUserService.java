package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.DeleteUserUseCase;
import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.DeleteUserPort;
import com.postech.restaurantes.application.port.out.TransactionPort;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Exclusão de usuário.
 *
 * <p>Confere a existência antes de excluir para que apagar um id inexistente
 * responda 404, e não um 204 que faria o cliente acreditar ter removido algo.</p>
 */
public class DeleteUserService implements DeleteUserUseCase {

    private final CheckUserExistsPort checkUserExistsPort;
    private final DeleteUserPort deleteUserPort;
    private final TransactionPort transactionPort;

    public DeleteUserService(CheckUserExistsPort checkUserExistsPort,
                             DeleteUserPort deleteUserPort,
                             TransactionPort transactionPort) {
        this.checkUserExistsPort = checkUserExistsPort;
        this.deleteUserPort = deleteUserPort;
        this.transactionPort = transactionPort;
    }

    @Override
    public void delete(UUID userId) {
        transactionPort.inTransaction(() -> {
            if (!checkUserExistsPort.existsById(userId)) {
                throw ResourceNotFoundException.user(userId);
            }
            deleteUserPort.deleteById(userId);
        });
    }
}

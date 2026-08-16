package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.DeleteUserPort;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private CheckUserExistsPort checkUserExistsPort;
    @Mock
    private DeleteUserPort deleteUserPort;

    private DeleteUserService service;

    @BeforeEach
    void setUp() {
        service = new DeleteUserService(checkUserExistsPort, deleteUserPort, new DirectTransactionPort());
    }

    @Test
    @DisplayName("exclui o usuário existente")
    void excluiExistente() {
        given(checkUserExistsPort.existsById(USER_ID)).willReturn(true);

        service.delete(USER_ID);

        verify(deleteUserPort).deleteById(USER_ID);
    }

    @Test
    @DisplayName("falha ao excluir id inexistente, em vez de fingir sucesso")
    void falhaInexistente() {
        given(checkUserExistsPort.existsById(USER_ID)).willReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(USER_ID));
        verify(deleteUserPort, never()).deleteById(any());
    }
}

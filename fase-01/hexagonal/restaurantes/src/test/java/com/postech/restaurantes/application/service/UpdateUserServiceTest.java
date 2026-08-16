package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.command.AddressCommand;
import com.postech.restaurantes.application.port.in.command.UpdateUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.LoadUserPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveUserPort saveUserPort;
    @Mock
    private CheckUserExistsPort checkUserExistsPort;

    private UpdateUserService service;

    @BeforeEach
    void setUp() {
        service = new UpdateUserService(loadUserPort, saveUserPort, checkUserExistsPort,
                new DirectTransactionPort());
    }

    private UpdateUserCommand comando(List<AddressCommand> enderecos) {
        return new UpdateUserCommand(USER_ID, "Maria Souza", "maria.souza@email.com",
                "maria.souza", enderecos);
    }

    @Test
    @DisplayName("atualiza os dados e substitui os endereços")
    void atualizaComSucesso() {
        User user = DomainFixtures.usuarioPersistido(USER_ID);
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.of(user));
        given(saveUserPort.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserView view = service.update(comando(List.of(
                new AddressCommand("Rua Nova", "10", null, "Centro", "Campinas", "SP", "13010-000"))));

        assertEquals("Maria Souza", view.name());
        assertEquals("maria.souza@email.com", view.email());
        assertEquals(1, view.addresses().size());
        assertEquals("13010-000", view.addresses().get(0).zipCode());
    }

    @Test
    @DisplayName("a senha permanece intacta — este endpoint não a altera")
    void naoAlteraSenha() {
        User user = DomainFixtures.usuarioPersistido(USER_ID);
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.of(user));
        given(saveUserPort.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        service.update(comando(List.of()));

        assertEquals(DomainFixtures.SENHA_HASH, user.getPassword());
    }

    @Test
    @DisplayName("lista de endereços vazia remove todos os anteriores")
    void listaVaziaRemoveEnderecos() {
        User user = DomainFixtures.usuarioPersistido(USER_ID);
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.of(user));
        given(saveUserPort.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        service.update(comando(List.of()));

        assertTrue(user.getAddresses().isEmpty());
    }

    @Test
    @DisplayName("a checagem de duplicidade ignora o próprio usuário")
    void ignoraOProprioUsuarioNaChecagem() {
        User user = DomainFixtures.usuarioPersistido(USER_ID);
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.of(user));
        given(saveUserPort.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        service.update(comando(List.of()));

        // Sem passar o id atual, salvar o cadastro sem trocar o e-mail acusaria
        // conflito do registro consigo mesmo.
        verify(checkUserExistsPort).existsByEmailExcluding("maria.souza@email.com", USER_ID);
        verify(checkUserExistsPort).existsByLoginExcluding("maria.souza", USER_ID);
    }

    @Test
    @DisplayName("recusa e-mail que já pertence a outro usuário")
    void recusaEmailDeOutroUsuario() {
        given(loadUserPort.findById(USER_ID))
                .willReturn(Optional.of(DomainFixtures.usuarioPersistido(USER_ID)));
        given(checkUserExistsPort.existsByEmailExcluding(anyString(), eq(USER_ID))).willReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.update(comando(List.of())));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("falha quando o usuário não existe")
    void falhaUsuarioInexistente() {
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(comando(List.of())));
    }
}

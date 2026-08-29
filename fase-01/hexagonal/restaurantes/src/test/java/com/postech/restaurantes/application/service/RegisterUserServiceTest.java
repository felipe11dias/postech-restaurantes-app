package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.in.command.AddressCommand;
import com.postech.restaurantes.application.port.in.command.RegisterUserCommand;
import com.postech.restaurantes.application.port.in.view.UserView;
import com.postech.restaurantes.application.port.out.CheckUserExistsPort;
import com.postech.restaurantes.application.port.out.LoadRolePort;
import com.postech.restaurantes.application.port.out.PasswordEncoderPort;
import com.postech.restaurantes.application.port.out.SaveUserPort;
import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.InvalidEmailException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.model.RoleName;
import com.postech.restaurantes.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Testes do cadastro de usuário.
 *
 * <p>Note a ausência de {@code @SpringBootTest} e de qualquer banco: o caso de uso
 * só conhece interfaces, então dublar as portas basta. Este é o benefício mais
 * tangível do hexágono — a suíte de regras de negócio roda em milissegundos.</p>
 */
@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private LoadRolePort loadRolePort;
    @Mock
    private SaveUserPort saveUserPort;
    @Mock
    private CheckUserExistsPort checkUserExistsPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(loadRolePort, saveUserPort, checkUserExistsPort,
                passwordEncoderPort, new DirectTransactionPort());
    }

    private RegisterUserCommand comando(Set<RoleName> papeis) {
        return new RegisterUserCommand("Maria Silva", "Maria@Email.COM", "maria.silva",
                "senha12345", papeis,
                List.of(new AddressCommand("Av. Paulista", "1500", null, "Bela Vista",
                        "São Paulo", "SP", "01310-200")));
    }

    @Test
    @DisplayName("cadastra o usuário com a senha em hash e o e-mail normalizado")
    void cadastraComSucesso() {
        given(loadRolePort.findByName(RoleName.ROLE_CUSTOMER))
                .willReturn(Optional.of(DomainFixtures.roleCustomer()));
        given(checkUserExistsPort.existsByEmailExcluding(anyString(), isNull())).willReturn(false);
        given(checkUserExistsPort.existsByLoginExcluding(anyString(), isNull())).willReturn(false);
        given(passwordEncoderPort.encode("senha12345")).willReturn(DomainFixtures.SENHA_HASH);
        given(saveUserPort.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserView view = service.register(comando(Set.of(RoleName.ROLE_CUSTOMER)));

        assertEquals("maria@email.com", view.email());
        assertEquals("Maria Silva", view.name());
        assertEquals(1, view.addresses().size());
        assertEquals("01310-200", view.addresses().get(0).zipCode());

        ArgumentCaptor<User> capturado = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(capturado.capture());
        assertEquals(DomainFixtures.SENHA_HASH, capturado.getValue().getPassword());
    }

    @Test
    @DisplayName("a checagem de duplicidade usa o e-mail já normalizado")
    void checaDuplicidadeComEmailNormalizado() {
        given(loadRolePort.findByName(RoleName.ROLE_CUSTOMER))
                .willReturn(Optional.of(DomainFixtures.roleCustomer()));
        given(passwordEncoderPort.encode(anyString())).willReturn(DomainFixtures.SENHA_HASH);
        given(saveUserPort.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.register(comando(Set.of(RoleName.ROLE_CUSTOMER)));

        // Entrou "Maria@Email.COM"; a consulta precisa sair em minúsculas, senão a
        // regra de e-mail único não pega variações de caixa.
        verify(checkUserExistsPort).existsByEmailExcluding(eq("maria@email.com"), isNull());
    }

    @Test
    @DisplayName("recusa e-mail já cadastrado")
    void recusaEmailDuplicado() {
        given(checkUserExistsPort.existsByEmailExcluding(anyString(), isNull())).willReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.register(comando(Set.of(RoleName.ROLE_CUSTOMER))));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("recusa login já cadastrado")
    void recusaLoginDuplicado() {
        given(checkUserExistsPort.existsByEmailExcluding(anyString(), isNull())).willReturn(false);
        given(checkUserExistsPort.existsByLoginExcluding(anyString(), isNull())).willReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.register(comando(Set.of(RoleName.ROLE_CUSTOMER))));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("impede o autocadastro com papel de administrador")
    void impedeAutocadastroAdmin() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.register(comando(Set.of(RoleName.ROLE_ADMIN))));

        // A guarda é a primeira coisa do caso de uso: nada é consultado nem gravado.
        verify(checkUserExistsPort, never()).existsByEmailExcluding(anyString(), any());
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("falha quando o papel informado não existe")
    void falhaPapelInexistente() {
        given(checkUserExistsPort.existsByEmailExcluding(anyString(), isNull())).willReturn(false);
        given(checkUserExistsPort.existsByLoginExcluding(anyString(), isNull())).willReturn(false);
        given(loadRolePort.findByName(RoleName.ROLE_OWNER)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.register(comando(Set.of(RoleName.ROLE_OWNER))));
    }

    @Test
    @DisplayName("rejeita e-mail malformado antes de qualquer acesso a dados")
    void rejeitaEmailMalformado() {
        RegisterUserCommand invalido = new RegisterUserCommand(
                "Maria", "nao-e-email", "maria", "senha12345",
                Set.of(RoleName.ROLE_CUSTOMER), List.of());

        assertThrows(InvalidEmailException.class, () -> service.register(invalido));
        verify(saveUserPort, never()).save(any());
    }

    /**
     * A guarda de autocadastro só se interessa por papéis privilegiados; a
     * ausência de papéis é recusada adiante, na resolução — mas o cadastro não
     * pode ser gravado em nenhum dos dois casos.
     */
    @Test
    @DisplayName("recusa cadastro sem papéis informados")
    void recusaSemPapeis() {
        given(checkUserExistsPort.existsByEmailExcluding(anyString(), isNull())).willReturn(false);
        given(checkUserExistsPort.existsByLoginExcluding(anyString(), isNull())).willReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.register(comando(Set.of())));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("recusa cadastro com a lista de papéis nula")
    void recusaPapeisNulos() {
        given(checkUserExistsPort.existsByEmailExcluding(anyString(), isNull())).willReturn(false);
        given(checkUserExistsPort.existsByLoginExcluding(anyString(), isNull())).willReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.register(comando(null)));
        verify(saveUserPort, never()).save(any());
    }

    /** Endereço é opcional no cadastro: sem lista, o usuário nasce sem endereços. */
    @Test
    @DisplayName("cadastra usuário sem endereços quando a lista vem nula")
    void cadastraSemEnderecos() {
        RegisterUserCommand semEnderecos = new RegisterUserCommand(
                "Maria Silva", "maria@email.com", "maria.silva", "senha12345",
                Set.of(RoleName.ROLE_CUSTOMER), null);

        given(checkUserExistsPort.existsByEmailExcluding(anyString(), isNull())).willReturn(false);
        given(checkUserExistsPort.existsByLoginExcluding(anyString(), isNull())).willReturn(false);
        given(loadRolePort.findByName(RoleName.ROLE_CUSTOMER))
                .willReturn(Optional.of(DomainFixtures.roleCustomer()));
        given(passwordEncoderPort.encode("senha12345")).willReturn(DomainFixtures.SENHA_HASH);
        given(saveUserPort.save(any())).willAnswer(call -> call.getArgument(0));

        service.register(semEnderecos);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertEquals(0, captor.getValue().getAddresses().size());
    }
}

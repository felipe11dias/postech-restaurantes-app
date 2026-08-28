package com.postech.restaurantes.service;

import com.postech.restaurantes.entity.Address;
import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.enums.RoleName;
import com.postech.restaurantes.exception.DuplicateResourceException;
import com.postech.restaurantes.exception.ForbiddenOperationException;
import com.postech.restaurantes.exception.InvalidPasswordException;
import com.postech.restaurantes.exception.ResourceNotFoundException;
import com.postech.restaurantes.mapper.AddressMapper;
import com.postech.restaurantes.mapper.UserMapper;
import com.postech.restaurantes.repository.RoleRepository;
import com.postech.restaurantes.repository.UserRepository;
import com.postech.restaurantes.vo.v1.request.AddressRequest;
import com.postech.restaurantes.vo.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.vo.v1.request.UserUpdateRequest;
import com.postech.restaurantes.vo.v1.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — regras de negócio de usuário")
class UserServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID MISSING_USER_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID ROLE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @Mock private AddressMapper addressMapper;

    @InjectMocks private UserService userService;

    private UserRegistrationRequest validRegistration() {
        return new UserRegistrationRequest(
                "João Silva", "joao@email.com", "joao.silva", "senhaSegura123",
                Set.of(RoleName.ROLE_CUSTOMER), List.of());
    }

    @Test
    @DisplayName("cadastra usuário válido, aplicando hash na senha")
    void register_comDadosValidos_devePersistir() {
        UserRegistrationRequest request = validRegistration();
        User entity = new User();
        UserResponse expected = new UserResponse(USER_ID, "João Silva", "joao@email.com",
                "joao.silva", Set.of(), List.of(), null, null);

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("HASHED");
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                .thenReturn(Optional.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER)));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        UserResponse result = userService.register(request);

        assertThat(result).isEqualTo(expected);
        verify(passwordEncoder).encode("senhaSegura123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("rejeita autocadastro solicitando o papel ROLE_ADMIN")
    void register_comPapelAdmin_deveLancar() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Hacker", "hacker@email.com", "hacker", "senhaSegura123",
                Set.of(RoleName.ROLE_ADMIN), List.of());

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("ROLE_ADMIN");

        verify(userRepository, never()).save(any());
    }

    /**
     * A regra de autocadastro só se interessa por papéis privilegiados; a
     * ausência de papéis é assunto do @NotEmpty no VO de entrada. Sem papéis, o
     * cadastro falha adiante — mas não como operação proibida, e nada é gravado.
     */
    @Test
    @DisplayName("cadastro sem papéis não é barrado pela regra de autocadastro")
    void register_semPapeis_naoDeveSerBarradoPelaRegraDeAutocadastro() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "João Silva", "joao@email.com", "joao.silva", "senhaSegura123", null, List.of());

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(new User());
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("HASHED");

        assertThatThrownBy(() -> userService.register(request))
                .isNotInstanceOf(ForbiddenOperationException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita cadastro com e-mail já existente")
    void register_comEmailDuplicado_deveLancar() {
        UserRegistrationRequest request = validRegistration();
        User existing = new User();
        existing.setId(OTHER_USER_ID);
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("e-mail");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita cadastro com login já existente")
    void register_comLoginDuplicado_deveLancar() {
        UserRegistrationRequest request = validRegistration();
        User existing = new User();
        existing.setId(OTHER_USER_ID);
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("login");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("troca de senha com senha atual correta e confirmação coincidente")
    void changePassword_valido_deveAtualizar() {
        User user = new User();
        user.setId(USER_ID);
        user.setPassword("HASH_ANTIGO");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("atual123", "HASH_ANTIGO")).thenReturn(true);
        when(passwordEncoder.encode("nova12345")).thenReturn("HASH_NOVO");

        userService.changePassword(USER_ID,
                new PasswordChangeRequest("atual123", "nova12345", "nova12345"));

        verify(passwordEncoder).encode("nova12345");
        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("HASH_NOVO");
    }

    @Test
    @DisplayName("troca de senha falha quando a senha atual está incorreta")
    void changePassword_senhaAtualIncorreta_deveLancar() {
        User user = new User();
        user.setId(USER_ID);
        user.setPassword("HASH_ANTIGO");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "HASH_ANTIGO")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(USER_ID,
                new PasswordChangeRequest("errada", "nova12345", "nova12345")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("atual");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("troca de senha falha quando a confirmação não coincide")
    void changePassword_confirmacaoDivergente_deveLancar() {
        User user = new User();
        user.setId(USER_ID);
        user.setPassword("HASH_ANTIGO");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("atual123", "HASH_ANTIGO")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(USER_ID,
                new PasswordChangeRequest("atual123", "nova12345", "diferente999")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("confirmação");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("consulta por id inexistente lança ResourceNotFound")
    void findById_inexistente_deveLancar() {
        when(userRepository.findById(MISSING_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(MISSING_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("exclusão de usuário inexistente lança ResourceNotFound")
    void delete_inexistente_deveLancar() {
        when(userRepository.existsById(MISSING_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(MISSING_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("busca por nome retorna a página de usuários mapeados")
    void findByName_deveRetornarPagina() {
        User user = new User();
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findByNameContainingIgnoreCase("jo", pageable))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toResponse(user)).thenReturn(
                new UserResponse(USER_ID, "João", "joao@email.com", "joao", Set.of(), List.of(), null, null));

        Page<UserResponse> result = userService.findByName("jo", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("João");
    }

    @Test
    @DisplayName("cadastro normaliza o CEP dos endereços informados")
    void register_comEnderecos_deveNormalizarOCep() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "João Silva", "joao@email.com", "joao.silva", "senhaSegura123",
                Set.of(RoleName.ROLE_CUSTOMER),
                List.of(new AddressRequest("Rua das Flores", "100", null, "Centro",
                        "Fortaleza", "CE", "60175-047")));

        User entity = new User();
        entity.getAddresses().add(Address.builder().street("Rua das Flores").zipCode("60175-047").build());

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("HASHED");
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                .thenReturn(Optional.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER)));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(USER_ID, "João Silva", "joao@email.com", "joao.silva",
                        Set.of(), List.of(), null, null));

        userService.register(request);

        assertThat(entity.getAddresses().get(0).getZipCode()).isEqualTo("60175047");
    }

    /** O e-mail é normalizado pelo VO Email antes de qualquer verificação. */
    @Test
    @DisplayName("cadastro normaliza o e-mail antes de conferir duplicidade")
    void register_deveNormalizarOEmail() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "João Silva", "  JOAO@Email.COM ", "joao.silva", "senhaSegura123",
                Set.of(RoleName.ROLE_CUSTOMER), List.of());
        User entity = new User();

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("HASHED");
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                .thenReturn(Optional.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER)));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(USER_ID, "João Silva", "joao@email.com", "joao.silva",
                        Set.of(), List.of(), null, null));

        userService.register(request);

        assertThat(entity.getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("cadastro com endereços nulos não quebra a normalização")
    void register_semEnderecos_deveCadastrar() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "João Silva", "joao@email.com", "joao.silva", "senhaSegura123",
                Set.of(RoleName.ROLE_CUSTOMER), null);
        User entity = new User();
        entity.setAddresses(null);

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("HASHED");
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                .thenReturn(Optional.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER)));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(USER_ID, "João Silva", "joao@email.com", "joao.silva",
                        Set.of(), List.of(), null, null));

        userService.register(request);

        verify(userRepository).save(entity);
    }

    @Test
    @DisplayName("cadastro com papel que não existe no banco lança ResourceNotFound")
    void register_comPapelInexistente_deveLancar() {
        UserRegistrationRequest request = validRegistration();
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(new User());
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("HASHED");
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Papel não encontrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualização substitui dados e endereços do usuário")
    void update_comDadosValidos_deveSubstituirEnderecos() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("João Silva");
        user.addAddress(Address.builder().street("Endereço antigo").build());

        UserUpdateRequest request = new UserUpdateRequest("João Atualizado", "novo@email.com",
                "joao.novo", List.of(new AddressRequest("Rua Nova", "200", null, "Centro",
                "Fortaleza", "CE", "60175-047")));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("novo@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.novo")).thenReturn(Optional.empty());
        when(addressMapper.toEntity(any(AddressRequest.class)))
                .thenReturn(Address.builder().street("Rua Nova").zipCode("60175-047").build());
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(USER_ID, "João Atualizado", "novo@email.com", "joao.novo",
                        Set.of(), List.of(), null, null));

        UserResponse result = userService.update(USER_ID, request);

        assertThat(result.name()).isEqualTo("João Atualizado");
        assertThat(user.getName()).isEqualTo("João Atualizado");
        assertThat(user.getEmail()).isEqualTo("novo@email.com");
        assertThat(user.getLogin()).isEqualTo("joao.novo");
        assertThat(user.getAddresses()).singleElement()
                .satisfies(address -> {
                    assertThat(address.getStreet()).isEqualTo("Rua Nova");
                    assertThat(address.getZipCode()).isEqualTo("60175047");
                    assertThat(address.getUserId()).isEqualTo(USER_ID);
                });
    }

    @Test
    @DisplayName("atualização sem lista de endereços deixa o usuário sem endereços")
    void update_semEnderecos_deveLimparAListaAntiga() {
        User user = new User();
        user.setId(USER_ID);
        user.addAddress(Address.builder().street("Endereço antigo").build());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(USER_ID, "João", "joao@email.com", "joao.silva",
                        Set.of(), List.of(), null, null));

        userService.update(USER_ID, new UserUpdateRequest("João", "joao@email.com", "joao.silva", null));

        assertThat(user.getAddresses()).isEmpty();
        verifyNoInteractions(addressMapper);
    }

    /**
     * Manter o próprio e-mail e o próprio login não é duplicidade: a checagem
     * ignora o registro que está sendo editado.
     */
    @Test
    @DisplayName("atualização que mantém o próprio e-mail e login é permitida")
    void update_mantendoOsProprios_naoDeveAcusarDuplicidade() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("joao@email.com");
        user.setLogin("joao.silva");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(USER_ID, "João", "joao@email.com", "joao.silva",
                        Set.of(), List.of(), null, null));

        userService.update(USER_ID, new UserUpdateRequest("João", "joao@email.com", "joao.silva", List.of()));

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("atualização com e-mail de outro usuário lança conflito")
    void update_comEmailDeOutro_deveLancar() {
        User user = new User();
        user.setId(USER_ID);
        User outro = new User();
        outro.setId(OTHER_USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("ocupado@email.com")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> userService.update(USER_ID,
                new UserUpdateRequest("João", "ocupado@email.com", "joao.silva", List.of())))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("e-mail");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualização com login de outro usuário lança conflito")
    void update_comLoginDeOutro_deveLancar() {
        User user = new User();
        user.setId(USER_ID);
        User outro = new User();
        outro.setId(OTHER_USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("ocupado")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> userService.update(USER_ID,
                new UserUpdateRequest("João", "joao@email.com", "ocupado", List.of())))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("login");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualização de usuário inexistente lança ResourceNotFound")
    void update_inexistente_deveLancar() {
        when(userRepository.findById(MISSING_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(MISSING_USER_ID,
                new UserUpdateRequest("João", "joao@email.com", "joao.silva", List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("troca de senha de usuário inexistente lança ResourceNotFound")
    void changePassword_inexistente_deveLancar() {
        when(userRepository.findById(MISSING_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(MISSING_USER_ID,
                new PasswordChangeRequest("atual123", "nova12345", "nova12345")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("exclusão de usuário existente remove o registro")
    void delete_existente_deveRemover() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userService.delete(USER_ID);

        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("consulta por id devolve o usuário mapeado")
    void findById_existente_deveDevolverOUsuario() {
        User user = new User();
        user.setId(USER_ID);
        UserResponse expected = new UserResponse(USER_ID, "João", "joao@email.com", "joao.silva",
                Set.of(), List.of(), null, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        assertThat(userService.findById(USER_ID)).isEqualTo(expected);
    }

    @Test
    @DisplayName("listagem devolve a página de usuários mapeados")
    void findAll_deveRetornarPagina() {
        User user = new User();
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toResponse(user)).thenReturn(
                new UserResponse(USER_ID, "João", "joao@email.com", "joao.silva",
                        Set.of(), List.of(), null, null));

        Page<UserResponse> result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).login()).isEqualTo("joao.silva");
    }
}

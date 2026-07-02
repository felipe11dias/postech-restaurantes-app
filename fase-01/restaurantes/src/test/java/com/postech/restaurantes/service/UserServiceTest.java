package com.postech.restaurantes.service;

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
import com.postech.restaurantes.vo.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — regras de negócio de usuário")
class UserServiceTest {

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
        UserResponse expected = new UserResponse(1L, "João Silva", "joao@email.com",
                "joao.silva", Set.of(), List.of(), null, null);

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("HASHED");
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                .thenReturn(Optional.of(new Role(2L, RoleName.ROLE_CUSTOMER)));
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

    @Test
    @DisplayName("rejeita cadastro com e-mail já existente")
    void register_comEmailDuplicado_deveLancar() {
        UserRegistrationRequest request = validRegistration();
        User existing = new User();
        existing.setId(99L);
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
        existing.setId(99L);
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
        user.setId(1L);
        user.setPassword("HASH_ANTIGO");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("atual123", "HASH_ANTIGO")).thenReturn(true);
        when(passwordEncoder.encode("nova12345")).thenReturn("HASH_NOVO");

        userService.changePassword(1L,
                new PasswordChangeRequest("atual123", "nova12345", "nova12345"));

        verify(passwordEncoder).encode("nova12345");
        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("HASH_NOVO");
    }

    @Test
    @DisplayName("troca de senha falha quando a senha atual está incorreta")
    void changePassword_senhaAtualIncorreta_deveLancar() {
        User user = new User();
        user.setId(1L);
        user.setPassword("HASH_ANTIGO");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "HASH_ANTIGO")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L,
                new PasswordChangeRequest("errada", "nova12345", "nova12345")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("atual");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("troca de senha falha quando a confirmação não coincide")
    void changePassword_confirmacaoDivergente_deveLancar() {
        User user = new User();
        user.setId(1L);
        user.setPassword("HASH_ANTIGO");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("atual123", "HASH_ANTIGO")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L,
                new PasswordChangeRequest("atual123", "nova12345", "diferente999")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("confirmação");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("consulta por id inexistente lança ResourceNotFound")
    void findById_inexistente_deveLancar() {
        when(userRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(123L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("exclusão de usuário inexistente lança ResourceNotFound")
    void delete_inexistente_deveLancar() {
        when(userRepository.existsById(123L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(123L))
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
                new UserResponse(1L, "João", "joao@email.com", "joao", Set.of(), List.of(), null, null));

        Page<UserResponse> result = userService.findByName("jo", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("João");
    }
}

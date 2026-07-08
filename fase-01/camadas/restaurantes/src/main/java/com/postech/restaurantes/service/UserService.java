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
import com.postech.restaurantes.vo.shared.Email;
import com.postech.restaurantes.vo.shared.ZipCode;
import com.postech.restaurantes.vo.v1.request.PasswordChangeRequest;
import com.postech.restaurantes.vo.v1.request.UserRegistrationRequest;
import com.postech.restaurantes.vo.v1.request.UserUpdateRequest;
import com.postech.restaurantes.vo.v1.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       AddressMapper addressMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
    }

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        ensureSelfRegistrationRolesAllowed(request.roles());

        String email = new Email(request.email()).value();
        ensureEmailIsAvailable(email, null);
        ensureLoginIsAvailable(request.login(), null);

        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(resolveRoles(request.roles()));
        normalizeAddresses(user);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findEntity(id);
        String email = new Email(request.email()).value();

        ensureEmailIsAvailable(email, user.getId());
        ensureLoginIsAvailable(request.login(), user.getId());

        user.setName(request.name());
        user.setEmail(email);
        user.setLogin(request.login());

        // Substitui os endereços (orphanRemoval remove os antigos)
        user.getAddresses().clear();
        if (request.addresses() != null) {
            request.addresses().forEach(addressRequest -> {
                Address address = addressMapper.toEntity(addressRequest);
                address.setZipCode(new ZipCode(address.getZipCode()).value());
                user.addAddress(address);
            });
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long id, PasswordChangeRequest request) {
        User user = findEntity(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("A senha atual está incorreta");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new InvalidPasswordException("A confirmação não coincide com a nova senha");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findByName(String name, Pageable pageable) {
        return userRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    // ----- private -----

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }

    private void ensureEmailIsAvailable(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Já existe um usuário com o e-mail " + email);
                });
    }

    private void ensureLoginIsAvailable(String login, Long currentUserId) {
        userRepository.findByLogin(login)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Já existe um usuário com o login " + login);
                });
    }

    /**
     * O cadastro é um endpoint público (auto-registro). Por isso, papéis
     * privilegiados não podem ser atribuídos por aqui — impede que qualquer
     * pessoa se registre como administrador. ROLE_ADMIN só é concedido via seed
     * ou por um fluxo administrativo autenticado.
     */
    private void ensureSelfRegistrationRolesAllowed(Set<RoleName> names) {
        if (names != null && names.contains(RoleName.ROLE_ADMIN)) {
            throw new ForbiddenOperationException(
                    "Não é permitido se autocadastrar com o papel " + RoleName.ROLE_ADMIN);
        }
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        return names.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Papel não encontrado: " + name)))
                .collect(Collectors.toSet());
    }

    private void normalizeAddresses(User user) {
        if (user.getAddresses() != null) {
            user.getAddresses().forEach(address ->
                    address.setZipCode(new ZipCode(address.getZipCode()).value()));
        }
    }
}

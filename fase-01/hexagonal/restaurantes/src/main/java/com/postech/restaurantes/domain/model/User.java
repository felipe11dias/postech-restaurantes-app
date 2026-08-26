package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.model.shared.Email;
import com.postech.restaurantes.domain.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Usuário do sistema — raiz do agregado, que reúne papéis e endereços.
 *
 * <p>Esta é uma classe de domínio pura: nenhuma anotação de framework, nenhuma
 * dependência de JPA, Spring ou Jackson. As regras que governam o ciclo de vida do
 * usuário (quando os timestamps nascem, o que uma atualização de perfil pode mexer,
 * o fato de a senha só mudar por um caminho próprio) moram aqui, e não espalhadas
 * pelos serviços ou pelo ORM.</p>
 *
 * <p>O estado só muda por métodos que expressam intenção de negócio
 * ({@code updateProfile}, {@code changePassword}, {@code replaceAddresses}) — não há
 * setters públicos. É o que impede, por exemplo, que uma alteração de nome esqueça
 * de registrar a data da última modificação.</p>
 */
public class User {

    private UUID id;
    private String name;
    private Email email;
    private String login;

    /** Sempre o hash — o domínio nunca vê nem guarda a senha em claro. */
    private String password;

    private final Set<Role> roles = new HashSet<>();
    private final List<Address> addresses = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    private User() {
    }

    /**
     * Cria um usuário novo, ainda sem identidade (o id é atribuído pelo banco no
     * momento da gravação). Recebe o hash da senha já calculado: codificar é papel
     * do adapter, através do {@code PasswordEncoderPort}.
     */
    public static User newUser(String name, Email email, String login, String passwordHash,
                               Set<Role> roles, List<Address> addresses) {
        User user = new User();
        user.name = ObjectUtils.requireNonBlank(name, "O nome é obrigatório");
        user.email = ObjectUtils.requireNonNull(email, "O e-mail é obrigatório");
        user.login = ObjectUtils.requireNonBlank(login, "O login é obrigatório");
        user.password = ObjectUtils.requireNonBlank(passwordHash, "A senha é obrigatória");

        if (ObjectUtils.isEmpty(roles)) {
            throw new IllegalArgumentException("O usuário deve ter ao menos um papel");
        }
        user.roles.addAll(roles);
        if (addresses != null) {
            addresses.forEach(user::addAddress);
        }

        LocalDateTime now = LocalDateTime.now();
        user.createdAt = now;
        user.lastUpdatedAt = now;
        return user;
    }

    /**
     * Reconstrói um usuário já existente a partir dos dados persistidos.
     *
     * <p>É o caminho usado exclusivamente pelo adapter de persistência ao ler do
     * banco: diferente de {@link #newUser}, não aplica as regras de criação nem
     * gera timestamps, porque o objeto já existia — apenas está voltando para a
     * memória com o estado que tinha.</p>
     */
    public static User restore(UUID id, String name, Email email, String login, String passwordHash,
                               Set<Role> roles, List<Address> addresses,
                               LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        User user = new User();
        user.id = id;
        user.name = name;
        user.email = email;
        user.login = login;
        user.password = passwordHash;
        if (roles != null) {
            user.roles.addAll(roles);
        }
        if (addresses != null) {
            user.addresses.addAll(addresses);
        }
        user.createdAt = createdAt;
        user.lastUpdatedAt = lastUpdatedAt;
        return user;
    }

    public UUID getId() {
        return id;
    }

    /** Atribuído uma única vez, pelo adapter de persistência, após o INSERT. */
    public void assignId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("O id do usuário já foi atribuído");
        }
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /**
     * Atualiza os dados cadastrais. A senha fica de fora de propósito: trocá-la é um
     * caso de uso exclusivo ({@link #changePassword(String)}), espelhando a separação
     * entre {@code PUT /users/{id}} e {@code PATCH /users/{id}/password} exigida pela fase.
     */
    public void updateProfile(String name, Email email, String login) {
        this.name = ObjectUtils.requireNonBlank(name, "O nome é obrigatório");
        this.email = ObjectUtils.requireNonNull(email, "O e-mail é obrigatório");
        this.login = ObjectUtils.requireNonBlank(login, "O login é obrigatório");
        touch();
    }

    /**
     * Recebe o hash já codificado: o domínio não conhece BCrypt — codificar é papel do
     * adapter via {@code PasswordEncoderPort}, e conferir a senha atual é regra do caso de uso.
     */
    public void changePassword(String newPasswordHash) {
        this.password = ObjectUtils.requireNonBlank(newPasswordHash, "A senha é obrigatória");
        touch();
    }

    public void addAddress(Address address) {
        addresses.add(ObjectUtils.requireNonNull(address, "O endereço é obrigatório"));
    }

    /**
     * Troca a lista inteira de endereços. É o que a atualização de perfil faz: os
     * endereços não têm identidade própria para o cliente da API — são enviados
     * sempre por completo, então substituir é mais fiel ao contrato do que tentar
     * casar item a item.
     */
    public void replaceAddresses(List<Address> newAddresses) {
        addresses.clear();
        if (newAddresses != null) {
            newAddresses.forEach(this::addAddress);
        }
        touch();
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(role -> role.getName() == roleName);
    }

    /** Toda mutação de estado registra o momento da última alteração. */
    private void touch() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Identidade de entidade: dois usuários são o mesmo se têm o mesmo id.
     * Usuários ainda não persistidos (id nulo) só são iguais a si mesmos.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other) || id == null || other.id == null) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

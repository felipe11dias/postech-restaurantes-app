package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.util.ObjectUtils;
import com.postech.restaurantes.domain.model.shared.Email;

import java.time.LocalDateTime;
import java.util.*;

public class User {

    private Long id;

    private String name;

    private Email email;

    private String login;

    private String password;

    private Set<Role> roles = new HashSet<>();

    private List<Address> addresses = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime lastUpdatedAt;

    public User(Long id, String name, Email email, String login, String password, Set<Role> roles, List<Address> addresses, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.login = login;
        this.password = password;
        this.roles = roles;
        this.addresses = addresses;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Long getId() {
        return id;
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
        return roles;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /** Timestamps de criação são regra de domínio: nascem aqui, não no ORM. */
    private void initTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastUpdatedAt = now;
    }

    /** Toda mutação de estado deve registrar o momento da última alteração. */
    private void touch() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Adiciona um endereço mantendo a consistência dos dois lados
     * do relacionamento (boa prática em relações bidirecionais).
     */
    public void addAddress(Address address) {
        addresses.add(address);
        address.setUser(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setUser(null);
    }

    public User newUser(String name, Email email, String login, String passwordHash,
                               Set<Role> roles, List<Address> addresses) {
        ObjectUtils.requireNonBlank(name, "O nome é obrigatório");
        ObjectUtils.requireNonBlank(login, "O login é obrigatório");
        ObjectUtils.requireNonBlank(passwordHash, "A senha é obrigatória");
        if (ObjectUtils.isEmpty(roles)) {
            throw new IllegalArgumentException("O usuário deve ter ao menos um papel");
        }

        User user = new User(null, name, email, login, passwordHash,
                new HashSet<>(roles),
                addresses == null ? new ArrayList<>() : new ArrayList<>(addresses),
                null, null);
        user.initTimestamps();
        return user;
    }

    /**
     * Atualiza os dados cadastrais. A senha fica de fora de propósito: trocá-la é um
     * caso de uso exclusivo ({@link #changePassword(String)}), espelhando a separação
     * PUT /users/{id} vs PATCH /users/{id}/password exigida pela fase.
     */
    public void updateProfile(String name, Email email) {
        this.name = ObjectUtils.requireNonBlank(name, "O nome é obrigatório");
        this.email = ObjectUtils.requireNonNull(email, "O e-mail é obrigatório");
        touch();
    }

    /**
     * Recebe o hash já codificado: o domínio não conhece BCrypt — codificar é papel do
     * adapter via PasswordEncoderPort, e conferir a senha atual é regra do caso de uso.
     */
    public void changePassword(String newPasswordHash) {
        this.password = ObjectUtils.requireNonBlank(newPasswordHash, "A senha é obrigatória");
        touch();
    }
}

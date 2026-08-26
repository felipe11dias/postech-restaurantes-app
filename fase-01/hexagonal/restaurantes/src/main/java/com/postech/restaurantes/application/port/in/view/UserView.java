package com.postech.restaurantes.application.port.in.view;

import com.postech.restaurantes.domain.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Projeção de saída de um usuário — o que os casos de uso devolvem.
 *
 * <p>As <em>views</em> existem para que a entidade de domínio não atravesse a
 * fronteira do hexágono. Devolver a {@code User} diretamente daria ao adapter web
 * acesso aos métodos que mudam estado ({@code changePassword}, {@code updateProfile})
 * e faria qualquer mudança no domínio virar, automaticamente, uma mudança no
 * contrato público da API.</p>
 *
 * <p>Note a ausência do campo de senha: o hash não sai do núcleo, então nenhum
 * adapter tem a chance de vazá-lo por descuido.</p>
 */
public record UserView(
        UUID id,
        String name,
        String email,
        String login,
        Set<RoleView> roles,
        List<AddressView> addresses,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {

    public static UserView from(User user) {
        return new UserView(
                user.getId(),
                user.getName(),
                user.getEmail().value(),
                user.getLogin(),
                user.getRoles().stream()
                        .map(RoleView::from)
                        .sorted(Comparator.comparing(RoleView::name))
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new)),
                user.getAddresses().stream().map(AddressView::from).toList(),
                user.getCreatedAt(),
                user.getLastUpdatedAt());
    }
}

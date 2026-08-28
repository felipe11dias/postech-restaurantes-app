package com.postech.restaurantes.security;

import com.postech.restaurantes.entity.Role;
import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.enums.RoleName;
import com.postech.restaurantes.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService — carga do usuário para o Spring Security")
class CustomUserDetailsServiceTest {

    private static final UUID ROLE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("converte os papéis do usuário em authorities")
    void loadUserByUsername_deveConverterPapeisEmAuthorities() {
        User user = User.builder()
                .login("joao.silva")
                .password("HASH")
                .roles(Set.of(new Role(ROLE_ID, RoleName.ROLE_CUSTOMER)))
                .build();
        when(userRepository.findByLogin("joao.silva")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("joao.silva");

        assertThat(details.getUsername()).isEqualTo("joao.silva");
        assertThat(details.getPassword()).isEqualTo("HASH");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("usuário sem papéis é carregado sem authorities")
    void loadUserByUsername_semPapeis_deveCarregarSemAuthorities() {
        User user = User.builder().login("sem.papel").password("HASH").build();
        when(userRepository.findByLogin("sem.papel")).thenReturn(Optional.of(user));

        assertThat(userDetailsService.loadUserByUsername("sem.papel").getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("login inexistente lança UsernameNotFoundException")
    void loadUserByUsername_inexistente_deveLancar() {
        when(userRepository.findByLogin("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("fantasma");
    }
}

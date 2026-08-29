package com.postech.restaurantes.security;

import com.postech.restaurantes.entity.User;
import com.postech.restaurantes.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Autorização por posse do recurso: sustenta as expressões @PreAuthorize dos
 * controllers, então cada caminho que devolve "false" é uma porta que fica
 * fechada — vale fixá-los todos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserSecurity — autorização por posse do recurso")
class UserSecurityTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Mock private UserRepository userRepository;

    @InjectMocks private UserSecurity userSecurity;

    private Authentication autenticado(String login) {
        return new UsernamePasswordAuthenticationToken(login, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    @DisplayName("o dono do registro é reconhecido")
    void isSelf_paraOProprioRegistro_deveSerVerdadeiro() {
        when(userRepository.findByLogin("joao.silva"))
                .thenReturn(Optional.of(User.builder().id(USER_ID).login("joao.silva").build()));

        assertThat(userSecurity.isSelf(USER_ID, autenticado("joao.silva"))).isTrue();
    }

    @Test
    @DisplayName("o registro de outro usuário é recusado")
    void isSelf_paraRegistroDeOutro_deveSerFalso() {
        when(userRepository.findByLogin("joao.silva"))
                .thenReturn(Optional.of(User.builder().id(OTHER_USER_ID).login("joao.silva").build()));

        assertThat(userSecurity.isSelf(USER_ID, autenticado("joao.silva"))).isFalse();
    }

    @Test
    @DisplayName("login autenticado sem usuário correspondente é recusado")
    void isSelf_semUsuarioCorrespondente_deveSerFalso() {
        when(userRepository.findByLogin("fantasma")).thenReturn(Optional.empty());

        assertThat(userSecurity.isSelf(USER_ID, autenticado("fantasma"))).isFalse();
    }

    @Test
    @DisplayName("id nulo é recusado sem consultar o repositório")
    void isSelf_comIdNulo_deveSerFalso() {
        assertThat(userSecurity.isSelf(null, autenticado("joao.silva"))).isFalse();

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("requisição sem autenticação é recusada sem consultar o repositório")
    void isSelf_semAutenticacao_deveSerFalso() {
        assertThat(userSecurity.isSelf(USER_ID, null)).isFalse();

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("autenticação não confirmada é recusada")
    void isSelf_comAutenticacaoNaoConfirmada_deveSerFalso() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("joao.silva", null);

        assertThat(userSecurity.isSelf(USER_ID, authentication)).isFalse();

        verify(userRepository, never()).findByLogin("joao.silva");
    }
}

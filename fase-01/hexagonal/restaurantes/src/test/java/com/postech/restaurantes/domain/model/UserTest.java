package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.DomainFixtures;
import com.postech.restaurantes.domain.model.shared.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes das regras que a entidade User protege por si mesma. */
class UserTest {

    @Nested
    @DisplayName("criação")
    class Criacao {

        @Test
        @DisplayName("registra createdAt e lastUpdatedAt no momento da criação")
        void registraTimestamps() {
            User user = DomainFixtures.novoUsuario();

            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getLastUpdatedAt());
            assertEquals(user.getCreatedAt(), user.getLastUpdatedAt());
        }

        @Test
        @DisplayName("exige ao menos um papel")
        void exigePapel() {
            assertThrows(IllegalArgumentException.class, () -> User.newUser(
                    "Maria", new Email("maria@email.com"), "maria",
                    DomainFixtures.SENHA_HASH, Set.of(), List.of()));
        }

        @Test
        @DisplayName("exige nome, login e senha preenchidos")
        void exigeCamposObrigatorios() {
            Set<Role> papeis = Set.of(DomainFixtures.roleCustomer());

            assertThrows(IllegalArgumentException.class, () -> User.newUser(
                    "  ", new Email("maria@email.com"), "maria", DomainFixtures.SENHA_HASH, papeis, List.of()));
            assertThrows(IllegalArgumentException.class, () -> User.newUser(
                    "Maria", new Email("maria@email.com"), "  ", DomainFixtures.SENHA_HASH, papeis, List.of()));
            assertThrows(IllegalArgumentException.class, () -> User.newUser(
                    "Maria", new Email("maria@email.com"), "maria", "  ", papeis, List.of()));
        }

        @Test
        @DisplayName("nasce sem id — a identidade vem do banco na gravação")
        void nasceSemId() {
            User user = DomainFixtures.novoUsuario();

            assertEquals(null, user.getId());
            UUID id = UUID.randomUUID();
            user.assignId(id);
            assertEquals(id, user.getId());
        }

        @Test
        @DisplayName("o id não pode ser reatribuído")
        void idNaoReatribuivel() {
            User user = DomainFixtures.novoUsuario();
            user.assignId(UUID.randomUUID());

            assertThrows(IllegalStateException.class, () -> user.assignId(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("alterações de estado")
    class Alteracoes {

        @Test
        @DisplayName("atualizar o perfil registra a data da última alteração")
        void atualizarPerfilRegistraData() throws InterruptedException {
            User user = DomainFixtures.usuarioPersistido();
            var antes = user.getLastUpdatedAt();
            Thread.sleep(1);

            user.updateProfile("Maria Souza", new Email("maria.souza@email.com"), "maria.souza");

            assertTrue(user.getLastUpdatedAt().isAfter(antes));
            assertEquals("Maria Souza", user.getName());
            assertEquals("maria.souza@email.com", user.getEmail().value());
        }

        @Test
        @DisplayName("atualizar o perfil não toca na senha — trocá-la é outro caminho")
        void atualizarPerfilNaoTocaSenha() {
            User user = DomainFixtures.usuarioPersistido();

            user.updateProfile("Maria Souza", new Email("maria.souza@email.com"), "maria.souza");

            assertEquals(DomainFixtures.SENHA_HASH, user.getPassword());
        }

        @Test
        @DisplayName("trocar a senha registra a data da última alteração")
        void trocarSenhaRegistraData() throws InterruptedException {
            User user = DomainFixtures.usuarioPersistido();
            var antes = user.getLastUpdatedAt();
            Thread.sleep(1);

            user.changePassword("$2a$10$novohash");

            assertEquals("$2a$10$novohash", user.getPassword());
            assertTrue(user.getLastUpdatedAt().isAfter(antes));
        }

        @Test
        @DisplayName("substituir endereços troca a lista inteira")
        void substituiEnderecos() {
            User user = DomainFixtures.usuarioPersistido();
            assertEquals(1, user.getAddresses().size());

            user.replaceAddresses(List.of());

            assertTrue(user.getAddresses().isEmpty());
        }

        @Test
        @DisplayName("a lista de endereços exposta é imutável — só muda pelos métodos da entidade")
        void listaDeEnderecosImutavel() {
            User user = DomainFixtures.usuarioPersistido();

            assertThrows(UnsupportedOperationException.class,
                    () -> user.getAddresses().add(DomainFixtures.endereco()));
        }
    }

    @Nested
    @DisplayName("papéis")
    class Papeis {

        @Test
        @DisplayName("reconhece o papel que possui e nega o que não possui")
        void reconhecePapel() {
            User user = DomainFixtures.usuarioPersistido();

            assertTrue(user.hasRole(RoleName.ROLE_CUSTOMER));
            assertFalse(user.hasRole(RoleName.ROLE_ADMIN));
        }
    }

    @Nested
    @DisplayName("listas ausentes")
    class ListasAusentes {

        @Test
        @DisplayName("criar sem endereços resulta em usuário sem endereços")
        void criarSemEnderecos() {
            User user = User.newUser("Maria", new Email("maria@email.com"), "maria",
                    DomainFixtures.SENHA_HASH, Set.of(DomainFixtures.roleCustomer()), null);

            assertTrue(user.getAddresses().isEmpty());
        }

        @Test
        @DisplayName("substituir endereços por nulo esvazia a lista, em vez de falhar")
        void substituirPorNulo() {
            User user = DomainFixtures.usuarioPersistido();

            user.replaceAddresses(null);

            assertTrue(user.getAddresses().isEmpty());
        }

        /**
         * O restore é alimentado pelo adapter de persistência, que monta o usuário
         * antes de anexar papéis e endereços — então precisa aceitar ambos nulos.
         */
        @Test
        @DisplayName("restore aceita papéis e endereços nulos")
        void restoreAceitaColecoesNulas() {
            User user = User.restore(UUID.randomUUID(), "Maria", new Email("maria@email.com"),
                    "maria", DomainFixtures.SENHA_HASH, null, null, null, null);

            assertTrue(user.getRoles().isEmpty());
            assertTrue(user.getAddresses().isEmpty());
        }
    }

    /**
     * Identidade de entidade: o que distingue dois usuários é o id, não os dados.
     * Um usuário ainda não persistido não tem identidade — e por isso não pode
     * ser considerado igual a nenhum outro, nem mesmo a outro sem id.
     */
    @Nested
    @DisplayName("identidade")
    class Identidade {

        @Test
        @DisplayName("usuários com o mesmo id são o mesmo usuário")
        void mesmoId() {
            UUID id = UUID.randomUUID();

            User um = DomainFixtures.usuarioPersistido(id);
            User outro = DomainFixtures.usuarioPersistido(id);

            assertEquals(um, outro);
            assertEquals(um.hashCode(), outro.hashCode());
        }

        @Test
        @DisplayName("usuários com ids diferentes são usuários diferentes")
        void idsDiferentes() {
            assertNotEquals(DomainFixtures.usuarioPersistido(), DomainFixtures.usuarioPersistido());
        }

        @Test
        @DisplayName("é igual a si mesmo")
        void igualASiMesmo() {
            User user = DomainFixtures.usuarioPersistido();

            assertEquals(user, user);
        }

        @Test
        @DisplayName("usuário sem id não é igual a nenhum outro")
        void semIdNaoEhIgualANinguem() {
            User semId = DomainFixtures.novoUsuario();

            assertNotEquals(semId, DomainFixtures.novoUsuario());
            assertNotEquals(semId, DomainFixtures.usuarioPersistido());
            assertNotEquals(DomainFixtures.usuarioPersistido(), semId);
            assertEquals(0, semId.hashCode());
        }

        @Test
        @DisplayName("não é igual a nulo nem a objeto de outro tipo")
        void outrosTipos() {
            User user = DomainFixtures.usuarioPersistido();

            assertNotEquals(user, null);
            assertNotEquals(user, "Maria Silva");
        }
    }
}

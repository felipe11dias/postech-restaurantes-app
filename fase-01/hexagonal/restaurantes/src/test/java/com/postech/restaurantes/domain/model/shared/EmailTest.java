package com.postech.restaurantes.domain.model.shared;

import com.postech.restaurantes.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do Value Object de e-mail.
 *
 * <p>Nenhum {@code @SpringBootTest}, nenhum mock: o domínio é Java puro, então
 * testá-lo custa milissegundos. É esse o retorno prático de manter o núcleo sem
 * framework.</p>
 */
class EmailTest {

    @Test
    @DisplayName("normaliza para minúsculas — é o que sustenta a regra de e-mail único")
    void normalizaParaMinusculas() {
        assertEquals("joao@email.com", new Email("Joao@Email.COM").value());
    }

    @Test
    @DisplayName("remove espaços nas pontas")
    void removeEspacos() {
        assertEquals("joao@email.com", new Email("  joao@email.com  ").value());
    }

    @Test
    @DisplayName("duas grafias do mesmo endereço comparam iguais")
    void grafiasDiferentesSaoIguais() {
        assertEquals(new Email("JOAO@email.com"), new Email("joao@email.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sem-arroba", "@sem-usuario.com", "usuario@", "usuario@sem-tld", "a b@c.com"})
    @DisplayName("rejeita e-mails malformados")
    void rejeitaMalformados(String valor) {
        assertThrows(InvalidEmailException.class, () -> new Email(valor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("rejeita e-mail ausente")
    void rejeitaAusente(String valor) {
        assertThrows(InvalidEmailException.class, () -> new Email(valor));
    }
}

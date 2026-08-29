package com.postech.restaurantes.domain.model;

import com.postech.restaurantes.domain.model.shared.ZipCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * O endereço cobra as próprias invariantes na construção — não existe endereço
 * de domínio sem rua, cidade, estado e CEP. Número e complemento continuam
 * opcionais porque nem todo endereço tem os dois.
 */
class AddressTest {

    private static final ZipCode CEP = new ZipCode("01310-200");

    @Nested
    @DisplayName("criação")
    class Criacao {

        @Test
        @DisplayName("endereço novo nasce sem id, com todos os dados preenchidos")
        void nasceSemId() {
            Address address = Address.newAddress("Av. Paulista", "1500", "Apto 42",
                    "Bela Vista", "São Paulo", "SP", CEP);

            assertNull(address.getId());
            assertEquals("Av. Paulista", address.getStreet());
            assertEquals("1500", address.getNumber());
            assertEquals("Apto 42", address.getComplement());
            assertEquals("Bela Vista", address.getNeighborhood());
            assertEquals("São Paulo", address.getCity());
            assertEquals("SP", address.getState());
            assertEquals(CEP, address.getZipCode());
        }

        @Test
        @DisplayName("número, complemento e bairro são opcionais")
        void camposOpcionais() {
            Address address = Address.newAddress("Av. Paulista", null, null, null,
                    "São Paulo", "SP", CEP);

            assertNull(address.getNumber());
            assertNull(address.getComplement());
            assertNull(address.getNeighborhood());
        }

        @Test
        @DisplayName("exige rua, cidade e estado preenchidos")
        void exigeCamposObrigatorios() {
            assertThrows(IllegalArgumentException.class, () -> Address.newAddress(
                    "  ", "1500", null, null, "São Paulo", "SP", CEP));
            assertThrows(IllegalArgumentException.class, () -> Address.newAddress(
                    "Av. Paulista", "1500", null, null, "  ", "SP", CEP));
            assertThrows(IllegalArgumentException.class, () -> Address.newAddress(
                    "Av. Paulista", "1500", null, null, "São Paulo", "  ", CEP));
        }

        @Test
        @DisplayName("exige CEP")
        void exigeCep() {
            assertThrows(IllegalArgumentException.class, () -> Address.newAddress(
                    "Av. Paulista", "1500", null, null, "São Paulo", "SP", null));
        }
    }

    @Nested
    @DisplayName("identidade")
    class Identidade {

        @Test
        @DisplayName("restore reconstrói o endereço já com id")
        void restoreTrazOId() {
            UUID id = UUID.randomUUID();

            Address address = Address.restore(id, "Av. Paulista", "1500", "Apto 42",
                    "Bela Vista", "São Paulo", "SP", CEP);

            assertEquals(id, address.getId());
        }

        @Test
        @DisplayName("o id é atribuído pelo adapter após o INSERT")
        void assignIdAposInsert() {
            Address address = Address.newAddress("Av. Paulista", "1500", null, null,
                    "São Paulo", "SP", CEP);
            UUID id = UUID.randomUUID();

            address.assignId(id);

            assertEquals(id, address.getId());
        }

        /** Reatribuir o id significaria trocar a identidade do registro. */
        @Test
        @DisplayName("o id não pode ser reatribuído")
        void idNaoReatribuivel() {
            Address address = Address.restore(UUID.randomUUID(), "Av. Paulista", "1500",
                    null, null, "São Paulo", "SP", CEP);

            assertThrows(IllegalStateException.class, () -> address.assignId(UUID.randomUUID()));
        }
    }
}

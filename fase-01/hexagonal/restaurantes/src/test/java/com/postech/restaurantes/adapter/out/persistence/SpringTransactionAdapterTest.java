package com.postech.restaurantes.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * O caso de uso apenas declara "isto aqui é uma unidade"; quem sabe o que é
 * commit e rollback é este adapter. O teste fixa a delegação — é ela que permite
 * trocar a tecnologia de transação sem tocar no núcleo.
 */
@ExtendWith(MockitoExtension.class)
class SpringTransactionAdapterTest {

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private SpringTransactionAdapter adapter;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("executa a ação dentro do template e devolve o resultado")
    void executaDentroDoTemplate() {
        given(transactionTemplate.execute(any(TransactionCallback.class)))
                .willAnswer(call -> ((TransactionCallback<String>) call.getArgument(0))
                        .doInTransaction(null));

        assertEquals("resultado", adapter.inTransaction(() -> "resultado"));
    }
}

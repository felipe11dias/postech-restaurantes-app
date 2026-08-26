package com.postech.restaurantes.adapter.out.persistence;

import com.postech.restaurantes.application.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Adapter de saída que realiza o {@link TransactionPort} com o gerenciamento de
 * transações do Spring.
 *
 * <p>Todo o conhecimento sobre commit, rollback e propagação fica confinado nestas
 * poucas linhas. O caso de uso apenas diz "isto aqui é uma unidade"; a tecnologia
 * que honra essa promessa é substituível sem que ele saiba.</p>
 */
@Component
public class SpringTransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}

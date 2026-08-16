package com.postech.restaurantes.application.service;

import com.postech.restaurantes.application.port.out.TransactionPort;

import java.util.function.Supplier;

/**
 * Implementação de teste do {@link TransactionPort}: executa a ação direto, sem
 * transação nenhuma.
 *
 * <p>Estas quatro linhas são o argumento prático da arquitetura. O que em produção
 * é um {@code TransactionTemplate} sobre uma conexão JDBC, no teste é uma chamada
 * de função — e o caso de uso não percebe a diferença, porque nunca dependeu do
 * Spring, apenas do contrato. É a Substituição de Liskov servindo a um propósito
 * concreto em vez de ficar na teoria.</p>
 */
class DirectTransactionPort implements TransactionPort {

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        return action.get();
    }
}

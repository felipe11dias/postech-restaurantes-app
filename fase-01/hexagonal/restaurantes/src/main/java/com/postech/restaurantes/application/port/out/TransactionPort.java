package com.postech.restaurantes.application.port.out;

import java.util.function.Supplier;

/**
 * Port de saída: execução de um bloco de trabalho como uma unidade atômica.
 *
 * <p>Existe por um motivo específico. Em uma arquitetura em camadas, a fronteira
 * transacional é declarada com {@code @Transactional} sobre o método do serviço —
 * mas aqui os serviços de aplicação são deliberadamente livres de anotações de
 * framework, e uma anotação do Spring dentro do hexágono seria justamente o
 * acoplamento que a arquitetura quer evitar.</p>
 *
 * <p>A alternativa era empurrar a transação para dentro do adapter de
 * persistência. Isso resolve os casos que tocam uma tabela só, mas não os que
 * coordenam dois ports — redefinir a senha grava o usuário <em>e</em> marca o
 * token como usado, e falhar entre as duas coisas deixaria um token gasto sem
 * senha nova. Quem sabe que essas duas gravações formam uma unidade é o caso de
 * uso, então é ele quem precisa delimitá-la; o <em>como</em> continua sendo do
 * adapter.</p>
 *
 * <p>Nos testes de caso de uso, a implementação é uma linha: executar a ação.</p>
 */
public interface TransactionPort {

    <T> T inTransaction(Supplier<T> action);

    default void inTransaction(Runnable action) {
        inTransaction(() -> {
            action.run();
            return null;
        });
    }
}

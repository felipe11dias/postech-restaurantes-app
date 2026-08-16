package com.postech.restaurantes.application.port.out;

/**
 * Port de saída: quem é o autor da gravação em curso, para as colunas de
 * auditoria {@code created_by} / {@code last_updated_by}.
 *
 * <p>Diferente dos demais ports, este não é consumido por nenhum caso de uso — é
 * o adapter de persistência que o chama antes de cada INSERT/UPDATE. A alternativa
 * seria o adapter de persistência ler o {@code SecurityContextHolder} diretamente,
 * o que colocaria conhecimento de autenticação dentro do adapter de banco e
 * amarraria os dois adapters entre si. Declarar a dependência como um port mantém
 * a persistência ignorante sobre como a identidade é estabelecida, e permite
 * substituí-la nos testes.</p>
 *
 * <p>Note que {@code createdBy}/{@code lastUpdatedBy} não aparecem no domínio: são
 * metadados de infraestrutura sobre a linha gravada, não fatos de negócio sobre o
 * usuário. Já {@code createdAt}/{@code lastUpdatedAt} são regra de domínio — a
 * fase exige a data da última alteração — e por isso vivem na entidade.</p>
 */
public interface AuditorPort {

    /** Identificação do autor; "system" quando não há usuário autenticado. */
    String currentAuditor();
}

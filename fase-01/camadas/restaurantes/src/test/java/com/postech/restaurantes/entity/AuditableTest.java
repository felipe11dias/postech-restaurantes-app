package com.postech.restaurantes.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sem JPA a auditoria deixou de ser automática: quem a preenche é o repositório,
 * chamando os métodos abaixo. Estes testes fixam esse contrato — em especial o de
 * que uma atualização jamais reescreve quem e quando criou o registro.
 */
@DisplayName("Auditable — campos de auditoria")
class AuditableTest {

    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 1, 10, 8, 0);
    private static final LocalDateTime ATUALIZACAO = LocalDateTime.of(2026, 2, 20, 17, 30);

    /** Qualquer entidade serve como sujeito; Role é a mais simples do modelo. */
    private final Auditable entidade = new Role();

    @Test
    @DisplayName("markCreated define criação e atualização com o mesmo auditor e momento")
    void markCreated_deveMarcarCriacaoEAtualizacao() {
        entidade.markCreated("system", CRIACAO);

        assertThat(entidade.getCreatedAt()).isEqualTo(CRIACAO);
        assertThat(entidade.getLastUpdatedAt()).isEqualTo(CRIACAO);
        assertThat(entidade.getCreatedBy()).isEqualTo("system");
        assertThat(entidade.getLastUpdatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("markUpdated preserva quem e quando criou o registro")
    void markUpdated_devePreservarACriacao() {
        entidade.markCreated("system", CRIACAO);

        entidade.markUpdated("cliente.demo", ATUALIZACAO);

        assertThat(entidade.getCreatedAt()).isEqualTo(CRIACAO);
        assertThat(entidade.getCreatedBy()).isEqualTo("system");
        assertThat(entidade.getLastUpdatedAt()).isEqualTo(ATUALIZACAO);
        assertThat(entidade.getLastUpdatedBy()).isEqualTo("cliente.demo");
    }

    @Test
    @DisplayName("restoreAudit repõe os quatro campos lidos do banco")
    void restoreAudit_deveReporValoresDoBanco() {
        entidade.restoreAudit(CRIACAO, ATUALIZACAO, "system", "admin");

        assertThat(entidade.getCreatedAt()).isEqualTo(CRIACAO);
        assertThat(entidade.getLastUpdatedAt()).isEqualTo(ATUALIZACAO);
        assertThat(entidade.getCreatedBy()).isEqualTo("system");
        assertThat(entidade.getLastUpdatedBy()).isEqualTo("admin");
    }
}

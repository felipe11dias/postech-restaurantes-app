package com.postech.restaurantes.entity;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Campos de auditoria comuns a todas as entidades (created_at / last_updated_at
 * / created_by / last_updated_by).
 *
 * Sem JPA não existe mais o preenchimento automático que o AuditingEntityListener
 * fazia: quem marca a criação e a atualização é o repositório, imediatamente
 * antes do INSERT/UPDATE. Por isso os campos não têm setters públicos — mudam
 * apenas através dos métodos abaixo, que expressam a intenção e impedem que uma
 * data de criação seja sobrescrita por engano.
 */
@Getter
public abstract class Auditable {

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private String createdBy;
    private String lastUpdatedBy;

    /** Marca a criação do registro. Chamado pelo repositório antes do INSERT. */
    public void markCreated(String auditor, LocalDateTime moment) {
        this.createdAt = moment;
        this.lastUpdatedAt = moment;
        this.createdBy = auditor;
        this.lastUpdatedBy = auditor;
    }

    /**
     * Marca a atualização do registro, preservando quem e quando criou.
     * Chamado pelo repositório antes do UPDATE.
     */
    public void markUpdated(String auditor, LocalDateTime moment) {
        this.lastUpdatedAt = moment;
        this.lastUpdatedBy = auditor;
    }

    /** Restaura os valores lidos do banco. Usado ao mapear o ResultSet. */
    public void restoreAudit(LocalDateTime createdAt, LocalDateTime lastUpdatedAt,
                             String createdBy, String lastUpdatedBy) {
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.createdBy = createdBy;
        this.lastUpdatedBy = lastUpdatedBy;
    }
}

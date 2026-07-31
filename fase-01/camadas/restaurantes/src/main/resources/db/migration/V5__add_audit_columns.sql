-- V5: Colunas de auditoria (created_by / last_updated_by) em todas as tabelas,
-- e created_at / last_updated_at nas tabelas que ainda não tinham, para suportar
-- a auditoria JPA (@EnableJpaAuditing / AuditorAware).

ALTER TABLE users
    ADD COLUMN created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN last_updated_by VARCHAR(255) NOT NULL DEFAULT 'system';

ALTER TABLE roles
    ADD COLUMN created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    ADD COLUMN last_updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    ADD COLUMN created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN last_updated_by VARCHAR(255) NOT NULL DEFAULT 'system';

ALTER TABLE addresses
    ADD COLUMN created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    ADD COLUMN last_updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    ADD COLUMN created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN last_updated_by VARCHAR(255) NOT NULL DEFAULT 'system';

ALTER TABLE password_reset_tokens
    ADD COLUMN last_updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    ADD COLUMN created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN last_updated_by VARCHAR(255) NOT NULL DEFAULT 'system';

-- A partir daqui, a aplicação sempre preenche esses campos via auditoria JPA
-- (mesmo padrão do restante do schema, que não usa defaults) — os defaults acima
-- servem só para popular as linhas já existentes.
ALTER TABLE users                 ALTER COLUMN created_by      DROP DEFAULT;
ALTER TABLE users                 ALTER COLUMN last_updated_by DROP DEFAULT;
ALTER TABLE roles                 ALTER COLUMN created_at      DROP DEFAULT;
ALTER TABLE roles                 ALTER COLUMN last_updated_at DROP DEFAULT;
ALTER TABLE roles                 ALTER COLUMN created_by      DROP DEFAULT;
ALTER TABLE roles                 ALTER COLUMN last_updated_by DROP DEFAULT;
ALTER TABLE addresses             ALTER COLUMN created_at      DROP DEFAULT;
ALTER TABLE addresses             ALTER COLUMN last_updated_at DROP DEFAULT;
ALTER TABLE addresses             ALTER COLUMN created_by      DROP DEFAULT;
ALTER TABLE addresses             ALTER COLUMN last_updated_by DROP DEFAULT;
ALTER TABLE password_reset_tokens ALTER COLUMN last_updated_at DROP DEFAULT;
ALTER TABLE password_reset_tokens ALTER COLUMN created_by      DROP DEFAULT;
ALTER TABLE password_reset_tokens ALTER COLUMN last_updated_by DROP DEFAULT;

-- V1: Schema inicial do sistema de gestão de restaurantes (Fase 1 — variante hexagonal)
--
-- Diferente da variante em camadas, cujo schema chegou ao formato atual por seis
-- migrations sucessivas (ids BIGINT convertidos para UUID, colunas de auditoria
-- adicionadas depois), aqui o banco é novo e nasce já no formato final. Replicar a
-- evolução histórica de outro banco em migrations de um banco que nunca existiu
-- seria ficção: migration registra a história real de UM schema.
--
-- Decisões carregadas da variante em camadas:
--   * PK em UUID gerado pelo banco (gen_random_uuid(), nativa do PostgreSQL 13+).
--     Ids sequenciais expostos em /api/v1/users/{id} permitem enumerar recursos.
--   * Colunas de auditoria (created_at/by, last_updated_at/by) em todas as tabelas.
--   * ON DELETE CASCADE nos vínculos do agregado de usuário.

CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    login           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    last_updated_at TIMESTAMP    NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    last_updated_by VARCHAR(255) NOT NULL
);

CREATE TABLE roles (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50)  NOT NULL UNIQUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    last_updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    last_updated_by VARCHAR(255) NOT NULL DEFAULT 'system'
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE addresses (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    street          VARCHAR(255) NOT NULL,
    number          VARCHAR(255),
    complement      VARCHAR(255),
    neighborhood    VARCHAR(255),
    city            VARCHAR(255) NOT NULL,
    state           VARCHAR(2)   NOT NULL,
    zip_code        VARCHAR(9)   NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    last_updated_at TIMESTAMP    NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    last_updated_by VARCHAR(255) NOT NULL
);

CREATE TABLE password_reset_tokens (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Apenas o hash do token: o valor em claro existe só no e-mail enviado.
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMP    NOT NULL,
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    last_updated_at TIMESTAMP    NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    last_updated_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);

-- Busca por nome (LOWER(name) LIKE ...) é o filtro da listagem paginada.
CREATE INDEX idx_users_name_lower ON users (LOWER(name));

-- Seed dos papéis de autorização. São dados de referência, não de demonstração:
-- sem eles nenhum cadastro consegue resolver seus papéis.
INSERT INTO roles (name) VALUES
    ('ROLE_OWNER'),
    ('ROLE_CUSTOMER'),
    ('ROLE_ADMIN');

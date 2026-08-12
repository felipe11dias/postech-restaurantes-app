-- V6: Converte as chaves primárias (e as chaves estrangeiras que as referenciam)
-- de BIGINT sequencial para UUID aleatório.
--
-- Motivação: ids sequenciais expostos nas rotas (/api/v1/users/{id}) permitem
-- enumeração de recursos — um cliente autenticado consegue varrer 1, 2, 3... e
-- inferir volume e existência de registros alheios. O UUID v4 (aleatório) elimina
-- essa previsibilidade.
--
-- O valor é gerado pelo banco, via DEFAULT gen_random_uuid() (função nativa do
-- PostgreSQL desde a versão 13, sem necessidade da extensão pgcrypto).
--
-- Estratégia: para cada coluna, cria-se a coluna UUID equivalente, migram-se os
-- vínculos existentes através dos ids antigos, e só então as colunas BIGINT são
-- descartadas. Assim os dados já cadastrados (inclusive os seeds V2/V3) são
-- preservados com seus relacionamentos intactos.

-- 1. Novas colunas de identidade nas tabelas com PK própria.
--    O DEFAULT já popula as linhas existentes no momento do ALTER.
ALTER TABLE users                 ADD COLUMN id_uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE roles                 ADD COLUMN id_uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE addresses             ADD COLUMN id_uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE password_reset_tokens ADD COLUMN id_uuid UUID NOT NULL DEFAULT gen_random_uuid();

-- 2. Novas colunas de chave estrangeira, ainda anuláveis (preenchidas no passo 3).
ALTER TABLE user_roles            ADD COLUMN user_id_uuid UUID;
ALTER TABLE user_roles            ADD COLUMN role_id_uuid UUID;
ALTER TABLE addresses             ADD COLUMN user_id_uuid UUID;
ALTER TABLE password_reset_tokens ADD COLUMN user_id_uuid UUID;

-- 3. Traduz os vínculos existentes: para cada FK antiga (BIGINT), copia o UUID
--    correspondente da tabela pai.
UPDATE user_roles ur
   SET user_id_uuid = u.id_uuid
  FROM users u
 WHERE ur.user_id = u.id;

UPDATE user_roles ur
   SET role_id_uuid = r.id_uuid
  FROM roles r
 WHERE ur.role_id = r.id;

UPDATE addresses a
   SET user_id_uuid = u.id_uuid
  FROM users u
 WHERE a.user_id = u.id;

UPDATE password_reset_tokens t
   SET user_id_uuid = u.id_uuid
  FROM users u
 WHERE t.user_id = u.id;

-- 4. Com os vínculos traduzidos, as FKs passam a ser obrigatórias (como as antigas).
ALTER TABLE user_roles            ALTER COLUMN user_id_uuid SET NOT NULL;
ALTER TABLE user_roles            ALTER COLUMN role_id_uuid SET NOT NULL;
ALTER TABLE addresses             ALTER COLUMN user_id_uuid SET NOT NULL;
ALTER TABLE password_reset_tokens ALTER COLUMN user_id_uuid SET NOT NULL;

-- 5. Remove as constraints que dependem das colunas BIGINT. As FKs vêm primeiro:
--    enquanto existirem, as PKs referenciadas não podem ser descartadas.
ALTER TABLE user_roles            DROP CONSTRAINT user_roles_user_id_fkey;
ALTER TABLE user_roles            DROP CONSTRAINT user_roles_role_id_fkey;
ALTER TABLE addresses             DROP CONSTRAINT addresses_user_id_fkey;
ALTER TABLE password_reset_tokens DROP CONSTRAINT password_reset_tokens_user_id_fkey;

ALTER TABLE user_roles            DROP CONSTRAINT user_roles_pkey;
ALTER TABLE users                 DROP CONSTRAINT users_pkey;
ALTER TABLE roles                 DROP CONSTRAINT roles_pkey;
ALTER TABLE addresses             DROP CONSTRAINT addresses_pkey;
ALTER TABLE password_reset_tokens DROP CONSTRAINT password_reset_tokens_pkey;

-- 6. Descarta as colunas BIGINT e promove as colunas UUID aos nomes originais.
ALTER TABLE user_roles            DROP COLUMN user_id;
ALTER TABLE user_roles            DROP COLUMN role_id;
ALTER TABLE addresses             DROP COLUMN user_id;
ALTER TABLE password_reset_tokens DROP COLUMN user_id;

ALTER TABLE users                 DROP COLUMN id;
ALTER TABLE roles                 DROP COLUMN id;
ALTER TABLE addresses             DROP COLUMN id;
ALTER TABLE password_reset_tokens DROP COLUMN id;

ALTER TABLE users                 RENAME COLUMN id_uuid      TO id;
ALTER TABLE roles                 RENAME COLUMN id_uuid      TO id;
ALTER TABLE addresses             RENAME COLUMN id_uuid      TO id;
ALTER TABLE password_reset_tokens RENAME COLUMN id_uuid      TO id;

ALTER TABLE user_roles            RENAME COLUMN user_id_uuid TO user_id;
ALTER TABLE user_roles            RENAME COLUMN role_id_uuid TO role_id;
ALTER TABLE addresses             RENAME COLUMN user_id_uuid TO user_id;
ALTER TABLE password_reset_tokens RENAME COLUMN user_id_uuid TO user_id;

-- 7. Recria as chaves primárias sobre as novas colunas.
ALTER TABLE users                 ADD PRIMARY KEY (id);
ALTER TABLE roles                 ADD PRIMARY KEY (id);
ALTER TABLE addresses             ADD PRIMARY KEY (id);
ALTER TABLE password_reset_tokens ADD PRIMARY KEY (id);
ALTER TABLE user_roles            ADD PRIMARY KEY (user_id, role_id);

-- 8. Recria as chaves estrangeiras, preservando o ON DELETE CASCADE original.
ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_role_id_fkey
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE;

ALTER TABLE addresses
    ADD CONSTRAINT addresses_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- 9. Recria o índice de busca por usuário (descartado junto da coluna antiga).
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);

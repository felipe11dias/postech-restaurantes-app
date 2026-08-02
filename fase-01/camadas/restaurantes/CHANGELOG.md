# Changelog — Schema do Banco de Dados

Histórico das migrations Flyway (`src/main/resources/db/migration`) do módulo
`camadas/restaurantes`. Toda nova migration `V<n>__descricao.sql` deve ganhar uma
entrada aqui.

## V5 — `V5__add_audit_columns.sql`
- Adiciona `created_by` e `last_updated_by` a `users`.
- Adiciona `created_at`, `last_updated_at`, `created_by` e `last_updated_by` a
  `roles` e `addresses` (que ainda não tinham nenhum rastro de auditoria).
- Adiciona `last_updated_at`, `created_by` e `last_updated_by` a
  `password_reset_tokens` (que já tinha `created_at`).
- Suporta a auditoria via Spring Data JPA (`@EnableJpaAuditing` +
  `AuditorAware`), que substitui os antigos `@PrePersist`/`@PreUpdate` manuais.

## V4 — `V4__create_password_reset_tokens.sql`
- Cria a tabela `password_reset_tokens`, para o fluxo de recuperação de senha
  por e-mail (token de uso único com expiração).

## V3 — `V3__seed_admin_user.sql`
- Popula o usuário de demonstração `admin.demo` com `ROLE_ADMIN`, usado nos
  cenários de escopo administrativo.

## V2 — `V2__seed_demo_users.sql`
- Popula os usuários de demonstração `dono.restaurante` (`ROLE_OWNER`) e
  `cliente.demo` (`ROLE_CUSTOMER`), com papéis e endereços.

## V1 — `V1__create_initial_schema.sql`
- Cria o schema inicial: `users`, `roles`, `user_roles`, `addresses`.
- Popula `roles` com `ROLE_OWNER`, `ROLE_CUSTOMER`, `ROLE_ADMIN`.

# Sistema de Gestão de Restaurantes — Tech Challenge Fase 1 (Arquitetura em Camadas)

Parte do monorepo [`postech`](../../../README.md) ▸ [Fase 1](../../README.md). Implementação
da **Fase 1** do Tech Challenge sob **arquitetura em camadas** (`controller` → `service` →
`repository`, SOLID + Clean Code) — esta é a **variante entregável oficial** da fase, com uma
variante comparativa em [Arquitetura Hexagonal](../../hexagonal/restaurantes/) (Ports &
Adapters, em andamento).

Backend em Spring Boot para gestão de usuários (donos de restaurante e clientes), com
autenticação JWT, banco PostgreSQL e orquestração via Docker Compose.

## Stack

- Java 21 · Spring Boot 3.5.x
- Spring Web, Spring JDBC (`JdbcTemplate`, sem ORM), Spring Validation, Spring Security
- PostgreSQL 16 · Flyway (migrations)
- springdoc-openapi (Swagger) · jjwt (JWT) · Spring Mail (recuperação de senha)
- MapStruct · Spring HATEOAS · Spring Boot Actuator
- JUnit 5 · Mockito · ArchUnit

## Pré-requisitos

- Docker e Docker Compose

## Como executar

```bash
# 1. (Opcional) criar seu .env a partir do exemplo
cp .env.example .env

# 2. Subir aplicação + banco
docker compose up --build
```

A aplicação sobe em `http://localhost:8080`.

```bash
# subir só o banco (desenvolvimento local)
docker compose up -d db

# encerrar
docker compose down

# encerrar e apagar os dados
docker compose down -v
```

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_NAME` | `restaurantes-app` | Nome do banco |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `DB_HOST` | `localhost` / `db` (compose) | Host do banco |
| `DB_PORT` | `5432` | Porta do banco |
| `JWT_SECRET` | *(exemplo)* | Segredo do JWT (mín. 256 bits) |
| `JWT_EXPIRATION` | `3600000` | Expiração do token (ms) |
| `MAIL_HOST` | `localhost` | Host do servidor SMTP |
| `MAIL_PORT` | `1025` | Porta do servidor SMTP |
| `MAIL_USERNAME` | *(vazio)* | Usuário SMTP (se o servidor exigir auth) |
| `MAIL_PASSWORD` | *(vazio)* | Senha SMTP (se o servidor exigir auth) |
| `MAIL_SMTP_AUTH` | `false` | Habilita autenticação SMTP |
| `MAIL_SMTP_STARTTLS` | `false` | Habilita STARTTLS |
| `MAIL_FROM` | `no-reply@restaurantes.postech` | Remetente dos e-mails |
| `MAIL_RESET_TOKEN_EXPIRATION_MINUTES` | `30` | Validade do token de redefinição de senha |

Os defaults de `MAIL_*` apontam para um SMTP local descartável (ex.: `python -m smtpd -c
DebuggingServer -n localhost:1025`), útil para testar a recuperação de senha sem credenciais
reais. Para um provedor real (Gmail, Mailtrap etc.), configure host/porta/usuário/senha e
habilite `MAIL_SMTP_AUTH`/`MAIL_SMTP_STARTTLS` conforme o provedor.

## Autenticação e autorização

A API é stateless e protegida por JWT. Fluxo:

1. Cadastre um usuário em `POST /api/v1/users` (público).
2. Autentique em `POST /api/v1/auth/login` para receber um token.
3. Envie o token nas demais chamadas: `Authorization: Bearer <token>`.

**Recuperação de senha (sem precisar da senha anterior):**

1. `POST /api/v1/auth/forgot-password` com o e-mail cadastrado — sempre responde `202`,
   exista ou não o e-mail (evita revelar quais e-mails estão cadastrados). Se existir, um
   token de uso único (validade configurável) é gerado e enviado por e-mail.
2. `POST /api/v1/auth/reset-password` com o token recebido e a nova senha — responde `204`
   em caso de sucesso, ou `400` se o token for inválido/expirado/já usado.

**Regras de autorização:**

- **Autocadastro restrito:** o cadastro público (`POST /api/v1/users`) **não** aceita o
  papel `ROLE_ADMIN` — a tentativa retorna `403`. Papéis de administrador só são concedidos
  via seed (migration) ou por um fluxo administrativo autenticado.
- **Posse do recurso:** as operações por id (`GET/PUT/PATCH/DELETE /api/v1/users/{id}`) só
  são permitidas ao **próprio usuário** (dono do recurso) ou a um usuário com `ROLE_ADMIN`.
  Acessar o recurso de outro usuário sem ser admin retorna `403`.

## Endpoints principais

| Método | Rota | Descrição | Autorização |
|--------|------|-----------|-------------|
| `POST` | `/api/v1/auth/login` | Validação de login (retorna JWT) | Pública |
| `POST` | `/api/v1/auth/forgot-password` | Solicita recuperação de senha por e-mail | Pública |
| `POST` | `/api/v1/auth/reset-password` | Redefine a senha a partir do token recebido | Pública |
| `POST` | `/api/v1/users` | Cadastro de usuário | Pública (sem `ROLE_ADMIN`) |
| `GET` | `/api/v1/users/{id}` | Consulta por id | Dono ou `ROLE_ADMIN` |
| `GET` | `/api/v1/users?name=...` | Busca paginada por nome | Autenticado |
| `GET` | `/api/v1/users` | Lista paginada (`page`, `size`, `sort`) | Autenticado |
| `PUT` | `/api/v1/users/{id}` | Atualiza dados (endpoint distinto) | Dono ou `ROLE_ADMIN` |
| `PATCH` | `/api/v1/users/{id}/password` | Troca de senha (endpoint exclusivo) | Dono ou `ROLE_ADMIN` |
| `DELETE` | `/api/v1/users/{id}` | Exclui | Dono ou `ROLE_ADMIN` |

## Documentação (Swagger)

Com a aplicação no ar: `http://localhost:8080/swagger-ui.html`
(use o botão **Authorize** para inserir o token JWT).

## Coleção Postman

Em `postman/Restaurantes.postman_collection.json`. Importe no Postman e execute de cima
para baixo: **Autenticação > Login admin** (salva `{{adminToken}}`), depois
**Recuperação de Senha > Cadastro para teste de recuperação** (salva `{{resetUserId}}`) e
**Usuários > Cadastro válido** (salva `{{userId}}`) e **Login do usuário criado** (salva
`{{token}}`). Os cenários de posse (`403`) e de admin (`200`/`404`) já vêm cobertos.

O caso de sucesso da redefinição de senha (**Recuperação de Senha > Redefinir senha —
sucesso**) depende da variável `{{resetToken}}`: como a API nunca retorna o token na resposta
(ele só vai por e-mail), copie o valor recebido — ou logado pela aplicação em INFO — após
executar **Esqueci minha senha — e-mail existente**, e cole em `{{resetToken}}` antes de rodar
a requisição de sucesso.

## Migrations e seeds (Flyway)

O schema e os dados iniciais são versionados com **Flyway**, aplicados automaticamente ao
subir a aplicação. O Flyway é a única fonte da verdade do schema — não há ORM gerando
nem validando DDL.

- Local dos scripts: `src/main/resources/db/migration`
- Nomenclatura: `V<versão>__<descrição>.sql`

| Versão | Conteúdo |
|--------|----------|
| `V1__create_initial_schema.sql` | Tabelas + seed dos papéis (`ROLE_OWNER`, `ROLE_CUSTOMER`, `ROLE_ADMIN`) |
| `V2__seed_demo_users.sql` | Usuários de demonstração (dono e cliente) com papéis e endereços |
| `V3__seed_admin_user.sql` | Usuário administrador de demonstração (`ROLE_ADMIN`) |
| `V4__create_password_reset_tokens.sql` | Tabela `password_reset_tokens` (recuperação de senha por e-mail) |
| `V5__add_audit_columns.sql` | Colunas de auditoria (`created_by`/`last_updated_by`/`created_at`/`last_updated_at`) em todas as tabelas |

Histórico detalhado de cada migration: [`CHANGELOG.md`](CHANGELOG.md).

**Usuários de demonstração** (para testar o login de imediato):

| Login | Senha | Papel |
|-------|-------|-------|
| `dono.restaurante` | `dono12345` | `ROLE_OWNER` |
| `cliente.demo` | `cliente12345` | `ROLE_CUSTOMER` |
| `admin.demo` | `admin12345` | `ROLE_ADMIN` |

**Criar uma nova migration:** adicione `V<n>__descricao.sql` (próximo número) em
`db/migration` e suba a aplicação. Migrations já aplicadas são **imutáveis** — nunca
edite uma existente; crie uma nova. O histórico fica na tabela `flyway_schema_history`.
Toda migration nova deve ganhar uma entrada no [`CHANGELOG.md`](CHANGELOG.md).

## Auditoria

Todas as entidades (`User`, `Role`, `Address`, `PasswordResetToken`) estendem a classe
base `Auditable`, preenchida em toda escrita:

- `created_at` / `last_updated_at`: data/hora de criação e da última alteração.
- `created_by` / `last_updated_by`: login do usuário autenticado no momento da escrita
  (via `SecurityContextHolder`, no bean `AuditorProvider`), ou `"system"`
  quando não há usuário autenticado (ex.: auto-cadastro público, seeds).

Sem ORM não há um listener que preencha esses campos sozinho: os repositórios chamam
`markCreated`/`markUpdated` imediatamente antes de cada INSERT/UPDATE. Por isso os campos
de `Auditable` não têm setters públicos — só mudam por esses métodos, o que impede que
uma data de criação seja sobrescrita por engano.

## Testes

```bash
mvn test
```

- **ArchUnit** (`ArchitectureTest`): valida as regras da arquitetura em camadas.
- **JUnit + Mockito** (`UserServiceTest`): cobre as regras de negócio do serviço de usuário.
- **JUnit** (`AuditorProviderTest`): cobre a resolução do auditor
  (usuário autenticado, contexto anônimo e ausência de autenticação).

## Estrutura

Arquitetura em camadas: `controller` → `service` → `repository`, com `entity`/`enums` no
domínio e `vo` (Value Objects versionados) nos contratos da API. Detalhes no
[relatório técnico da fase](../../relatorios/camadas/relatorio-tech-challenge-fase01-v1.1.md).

> Esta é a variante **em camadas**. A mesma Fase 1 também é implementada com
> [arquitetura hexagonal](../../hexagonal/restaurantes/) como desafio de estudo.

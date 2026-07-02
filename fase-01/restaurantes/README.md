# Sistema de Gestão de Restaurantes — Tech Challenge Fase 1

Backend em Spring Boot para gestão de usuários (donos de restaurante e clientes), com
autenticação JWT, banco PostgreSQL e orquestração via Docker Compose.

## Stack

- Java 21 · Spring Boot 3.5.x
- Spring Web, Spring Data JPA, Spring Validation, Spring Security
- PostgreSQL 16 · Flyway (migrations)
- springdoc-openapi (Swagger) · jjwt (JWT)
- MapStruct · Lombok · Spring HATEOAS · Spring Boot Actuator
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

## Autenticação e autorização

A API é stateless e protegida por JWT. Fluxo:

1. Cadastre um usuário em `POST /api/v1/users` (público).
2. Autentique em `POST /api/v1/auth/login` para receber um token.
3. Envie o token nas demais chamadas: `Authorization: Bearer <token>`.

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
**Usuários > Cadastro válido** (salva `{{userId}}`) e **Login do usuário criado** (salva
`{{token}}`). Os cenários de posse (`403`) e de admin (`200`/`404`) já vêm cobertos.

## Migrations e seeds (Flyway)

O schema e os dados iniciais são versionados com **Flyway**, aplicados automaticamente ao
subir a aplicação. O Hibernate roda em `validate` (não altera o banco).

- Local dos scripts: `src/main/resources/db/migration`
- Nomenclatura: `V<versão>__<descrição>.sql`

| Versão | Conteúdo |
|--------|----------|
| `V1__create_initial_schema.sql` | Tabelas + seed dos papéis (`ROLE_OWNER`, `ROLE_CUSTOMER`, `ROLE_ADMIN`) |
| `V2__seed_demo_users.sql` | Usuários de demonstração (dono e cliente) com papéis e endereços |
| `V3__seed_admin_user.sql` | Usuário administrador de demonstração (`ROLE_ADMIN`) |

**Usuários de demonstração** (para testar o login de imediato):

| Login | Senha | Papel |
|-------|-------|-------|
| `dono.restaurante` | `dono12345` | `ROLE_OWNER` |
| `cliente.demo` | `cliente12345` | `ROLE_CUSTOMER` |
| `admin.demo` | `admin12345` | `ROLE_ADMIN` |

**Criar uma nova migration:** adicione `V<n>__descricao.sql` (próximo número) em
`db/migration` e suba a aplicação. Migrations já aplicadas são **imutáveis** — nunca
edite uma existente; crie uma nova. O histórico fica na tabela `flyway_schema_history`.

## Testes

```bash
mvn test
```

- **ArchUnit** (`ArchitectureTest`): valida as regras da arquitetura em camadas.
- **JUnit + Mockito** (`UserServiceTest`): cobre as regras de negócio do serviço de usuário.

## Estrutura

Arquitetura em camadas: `controller` → `service` → `repository`, com `entity`/`enums` no
domínio e `vo` (Value Objects versionados) nos contratos da API. Detalhes no relatório
técnico da fase.

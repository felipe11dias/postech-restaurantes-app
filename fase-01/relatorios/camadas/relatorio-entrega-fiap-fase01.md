# Tech Challenge — Fase 1

## Sistema de Gestão de Restaurantes

**Autores:** Rafael Brandão · Felipe Dias Mac Dowell · Rodrigo do Carmo Cruz · Francisco Ferreira · Ana Melissa Lima do Nascimento

**Data de entrega:** 01/09/2026

**Repositório (público):** https://github.com/felipe11dias/postech-restaurantes-app

**Entregável oficial:** [`fase-01/camadas/restaurantes/`](https://github.com/felipe11dias/postech-restaurantes-app/tree/main/fase-01/camadas/restaurantes)

---

## 1. Introdução

Este documento descreve as atividades desenvolvidas no projeto **Sistema de Gestão de
Restaurantes**, elaborado pela equipe responsável pelo Tech Challenge da Pós-Tech em
Arquitetura e Desenvolvimento Java. O projeto atende à necessidade de gerenciamento de
usuários em um sistema de restaurantes, por meio de uma API REST que centraliza cadastro,
autenticação e administração de clientes e proprietários de restaurantes.

A solução foi desenvolvida em **Java 21**, **Spring Boot 3.5.11**, **PostgreSQL 16** e
**Docker Compose**, aplicando boas práticas de arquitetura, segurança e organização de
código.

Além do entregável oficial em **arquitetura em camadas**, a equipe implementou a mesma
Fase 1 em **arquitetura hexagonal** (Ports & Adapters) como exercício comparativo, sobre a
mesma stack. A seção 8 registra essa comparação; ela não substitui o entregável oficial.

---

## 2. Definição do Problema

Restaurantes que utilizam soluções digitais precisam de um sistema confiável para gerenciar
o cadastro e a autenticação de usuários, garantindo que clientes e proprietários tenham
acesso seguro às funcionalidades disponíveis. A ausência de um processo estruturado pode
resultar em falhas de segurança, dificuldade na administração de usuários e baixa
confiabilidade no acesso ao sistema.

Diante desse cenário, o projeto propõe uma API REST responsável pelo gerenciamento de
usuários, contemplando cadastro, autenticação e controle de acesso por perfis. A solução
oferece uma base segura, organizada e escalável para as próximas fases.

---

## 3. Levantamento de Requisitos

### 3.1 Event Storming

O levantamento foi feito pela identificação dos principais eventos de negócio relacionados
ao gerenciamento de usuários. A partir dos fluxos da aplicação, foram mapeados os comandos
executados pelos usuários, os eventos gerados e os agregados responsáveis por manter a
consistência das informações.

| Elemento | Itens mapeados |
| --- | --- |
| **Comandos** | Cadastrar usuário; Realizar login; Buscar usuário; Atualizar dados; Alterar senha; Recuperar senha; Excluir usuário |
| **Eventos** | Usuário cadastrado; Usuário autenticado; Usuário atualizado; Senha alterada; Token de recuperação emitido; Usuário removido; Acesso autorizado ou negado |
| **Agregados** | Usuário; Endereço; Perfil de acesso (Role); Token de redefinição de senha |

**Fluxo principal**

1. O usuário realiza o cadastro no sistema (endpoint público).
2. Efetua a autenticação para obtenção do token JWT.
3. Utiliza o token para acessar as funcionalidades protegidas.
4. Gerencia seus próprios dados, incluindo atualização de informações e alteração de senha.
5. Administradores possuem permissão para gerenciar qualquer usuário do sistema.

### 3.2 Mapeamento de Demandas e Necessidades

O sistema possui três perfis de acesso, definidos pela entidade `Role` e persistidos no
banco:

| Perfil | Papel | Necessidade atendida |
| --- | --- | --- |
| **Cliente** | `ROLE_CUSTOMER` | Cadastrar-se, autenticar-se, consultar e atualizar os próprios dados, alterar a senha e excluir a própria conta |
| **Dono do Restaurante** | `ROLE_OWNER` | Perfil previsto na modelagem de autorização, representando o responsável pelo restaurante. Na Fase 1 o escopo está concentrado em usuários, autenticação e autorização — funcionalidades específicas de restaurante entram nas fases seguintes |
| **Administrador** | `ROLE_ADMIN` | Consultar, atualizar e excluir usuários diferentes do autenticado, conforme as regras de autorização |

O cadastro público **não permite** que o usuário atribua a si próprio o papel `ROLE_ADMIN`
— a tentativa resulta em `403`. O papel administrativo vem do seed (migration `V3`) ou de um
fluxo autenticado autorizado.

### 3.3 Identificação dos Agregados

| Agregado / Entidade | Papel no modelo |
| --- | --- |
| **User** (raiz do agregado) | Identificação, credenciais, papéis e endereços do usuário |
| **Address** | Endereços do usuário. Relação 1:N, dependente do usuário |
| **Role** | Papéis de autorização, compartilhados entre usuários via tabela associativa `user_roles` (N:N) |
| **PasswordResetToken** | Tokens de recuperação de senha. Pertence a um usuário, tem prazo de expiração e uso único |

**Relações:** `User` 1—N `Address` · `User` N—N `Role` · `User` 1—N `PasswordResetToken`.

---

## 4. Arquitetura do Sistema

### 4.1 Abordagem Domain-Driven Design

O projeto utiliza conceitos de DDD para organizar entidades, responsabilidades e regras. O
agregado `User` é o núcleo do modelo, relacionando-se com `Address`, `Role` e
`PasswordResetToken`.

A implementação adota **arquitetura em camadas**, separando `controller`, `service`,
`repository` e `entity`. Os controllers expõem a API REST, os services concentram as regras
de negócio, os repositories abstraem o acesso ao PostgreSQL e as entidades representam o
modelo. Componentes transversais — segurança JWT, tratamento de exceções, mapeamento e
configuração — complementam a estrutura.

As regras de camada não ficam só na documentação: são **verificadas automaticamente no
build** por 5 regras de ArchUnit (`ArchitectureTest`), que reprovam a compilação se um
controller acessar um repository diretamente, se uma entidade depender de VO ou controller,
ou se o contrato do pacote `repository` deixar de ser de interfaces.

### 4.2 Definição dos Domínios

| # | Domínio | Responsabilidade |
| --- | --- | --- |
| 1 | **Usuários** (núcleo) | Cadastro, consulta, atualização, alteração de senha e exclusão. Concentra unicidade de e-mail e login, normalização de entrada, hash BCrypt da senha, associação a papéis e endereços e registro de auditoria |
| 2 | **Autenticação e Segurança** | Login, emissão e validação de JWT, autorização por papel e por posse do recurso |
| 3 | **Perfis de Acesso** | Definição dos papéis `ROLE_CUSTOMER`, `ROLE_OWNER` e `ROLE_ADMIN`, persistidos em `roles` e vinculados por `user_roles` |
| 4 | **Endereços** | Endereços do usuário (rua, número, complemento, bairro, cidade, estado, CEP), removidos junto com o usuário |
| 5 | **Recuperação de Senha** | Solicitação por e-mail e redefinição por token temporário, armazenado como hash SHA-256, com expiração e uso único |

**Interação entre domínios.** No cadastro, o domínio de Usuários valida os dados, associa os
papéis permitidos e registra os endereços. No login, o domínio de Autenticação valida as
credenciais e gera o JWT, usado nas requisições seguintes. Nas operações sobre um usuário
específico, o sistema verifica se o autenticado é o dono do recurso ou um administrador.

### 4.3 Modelagem Conceitual

**Decisões de modelagem registradas:**

- **Sem herança entre tipos de usuário.** O enunciado permite herança ou não. A equipe optou
  por **composição via papéis**: um único `User` com `Set<Role>`. Herança (`Cliente extends
  Usuario`) impediria um mesmo usuário de acumular papéis e forçaria estratégias de
  mapeamento relacional que não trazem ganho neste escopo.
- **Endereço como entidade própria**, não como atributos embutidos em `User`. O enunciado
  aceita as duas formas; a tabela separada normaliza o modelo e suporta 1:N.
- **Papéis como tabela, não enum de coluna.** Permite consultar e evoluir os papéis sem
  migration de tipo.
- **Chaves primárias em UUID** (migration `V6`), evitando ids sequenciais previsíveis em
  recursos expostos na URL.
- **Sem ORM.** O acesso a dados usa JDBC puro com `JdbcTemplate`. O que o ORM fazia
  implicitamente — *cascade*, *orphanRemoval*, tabela associativa e auditoria — passou a ser
  SQL explícito no repositório.

---

## 5. Documentação do Projeto

### 5.1 Estrutura do repositório

```
postech-restaurantes-app/
├── README.md                          # índice do monorepo
└── fase-01/
    ├── README.md                      # índice da fase, com a comparação entre variantes
    ├── camadas/restaurantes/          # ENTREGÁVEL OFICIAL (arquitetura em camadas)
    │   ├── Dockerfile                 # multi-stage
    │   ├── docker-compose.yml         # serviço de banco + aplicação
    │   ├── pom.xml
    │   ├── postman/
    │   │   ├── Restaurantes.postman_collection.json
    │   │   ├── prints/                # 47 prints das respostas
    │   │   └── gerador/               # script que gera a collection
    │   └── src/main/resources/db/migration/   # V1..V6 (Flyway)
    ├── hexagonal/restaurantes/        # variante comparativa (Ports & Adapters)
    └── relatorios/
        ├── camadas/                   # relatórios do entregável oficial
        └── hexagonal/                 # relatórios da variante
```

### 5.2 Modelo de dados

| Tabela | Conteúdo | Observações |
| --- | --- | --- |
| `users` | id (UUID), name, email, login, password | `email` e `login` com constraint `UNIQUE` |
| `addresses` | id, user_id, street, number, complement, neighborhood, city, state, zip_code | FK para `users` com `ON DELETE CASCADE` |
| `roles` | id, name | `name` único; seed com os três papéis |
| `user_roles` | user_id, role_id | Tabela associativa N:N |
| `password_reset_tokens` | id, user_id, token_hash, expires_at, used | Token guardado como hash SHA-256 |

Todas as tabelas possuem as quatro colunas de auditoria: `created_at`, `last_updated_at`,
`created_by`, `last_updated_by`.

### 5.3 Migrations (Flyway)

| Migration | Conteúdo |
| --- | --- |
| `V1__create_initial_schema.sql` | Schema inicial: `users`, `roles`, `user_roles`, `addresses` |
| `V2__seed_demo_users.sql` | Usuários de demonstração |
| `V3__seed_admin_user.sql` | Usuário administrador (origem do `ROLE_ADMIN`) |
| `V4__create_password_reset_tokens.sql` | Tabela de tokens de recuperação |
| `V5__add_audit_columns.sql` | Colunas de auditoria em todas as tabelas |
| `V6__convert_ids_to_uuid.sql` | Conversão das chaves primárias para UUID |

### 5.4 Execução com Docker Compose

```bash
# na raiz de fase-01/camadas/restaurantes
docker compose up --build        # sobe banco + aplicação
docker compose up db             # sobe apenas o banco (útil no desenvolvimento)
docker compose down              # encerra
docker compose down -v           # encerra e apaga os dados
```

A aplicação sobe em `http://localhost:8080`; o banco em `localhost:5432`. O `compose`
declara um *healthcheck* no banco e só inicia a aplicação quando ele responde
(`depends_on: condition: service_healthy`). Todas as credenciais e segredos vêm de
variáveis de ambiente, com defaults para desenvolvimento local.

---

## 6. Implementação

### 6.1 Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem / plataforma | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Persistência | PostgreSQL 16 · JDBC puro (`JdbcTemplate`) · Flyway |
| Segurança | Spring Security · JWT (jjwt 0.12.6) · BCrypt |
| Mapeamento | MapStruct 1.6.3 |
| Documentação | springdoc-openapi 2.8.17 (Swagger UI) |
| Observabilidade | Spring Boot Actuator |
| Testes | JUnit 5 · Mockito · AssertJ · ArchUnit 1.4.2 · JaCoCo 0.8.13 |
| Infraestrutura | Docker (multi-stage) · Docker Compose |

### 6.2 Fluxos principais

**Cadastro e autenticação.** O usuário se cadastra informando dados pessoais, endereço e
perfil permitido. O sistema valida e persiste. Em seguida faz login com login e senha; em
caso de sucesso recebe um token JWT usado nos endpoints protegidos.

**Gerenciamento.** O usuário autenticado envia o token; o sistema valida as permissões antes
de cada operação. Usuários comuns operam os próprios dados; administradores operam qualquer
usuário.

**Recuperação de senha.** O usuário solicita a recuperação por e-mail e recebe um token
opaco. A API responde `202` **exista ou não o e-mail informado** — responder `404` para
e-mail inexistente transformaria o endpoint público em uma forma de descobrir quais contas
existem. O token é persistido apenas como hash SHA-256, expira e só pode ser usado uma vez.

### 6.3 Rotas disponíveis

Todas as rotas são versionadas sob o prefixo `/api/v1`.

**Autenticação**

| Método | Rota | Descrição | Acesso |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Autentica e retorna o token JWT | Público |
| `POST` | `/api/v1/auth/forgot-password` | Solicita recuperação de senha | Público |
| `POST` | `/api/v1/auth/reset-password` | Redefine a senha por token | Público |

**Usuários**

| Método | Rota | Descrição | Acesso |
| --- | --- | --- | --- |
| `POST` | `/api/v1/users` | Cadastro de novo usuário | Público |
| `GET` | `/api/v1/users` | Lista usuários de forma paginada | Autenticado |
| `GET` | `/api/v1/users?name={nome}` | Busca usuários por nome | Autenticado |
| `GET` | `/api/v1/users/{id}` | Consulta usuário pelo UUID | Próprio usuário ou `ROLE_ADMIN` |
| `PUT` | `/api/v1/users/{id}` | Atualiza os dados do usuário | Próprio usuário ou `ROLE_ADMIN` |
| `PATCH` | `/api/v1/users/{id}/password` | Altera a senha | Próprio usuário ou `ROLE_ADMIN` |
| `DELETE` | `/api/v1/users/{id}` | Exclui o usuário | Próprio usuário ou `ROLE_ADMIN` |

### 6.4 Tratamento de erros

Toda resposta de erro segue o padrão **ProblemDetail (RFC 7807)**, com um `type` estável por
categoria — para que o cliente diferencie o erro de forma programática em vez de depender do
título em texto livre.

| Situação | Status | `type` |
| --- | --- | --- |
| Recurso não encontrado | `404` | `/problemas/recurso-nao-encontrado` |
| E-mail ou login duplicado | `409` | `/problemas/conflito-de-dados` |
| Senha inválida | `400` | `/problemas/senha-invalida` |
| Token de recuperação inválido/expirado | `400` | `/problemas/token-invalido-ou-expirado` |
| Autocadastro com papel privilegiado | `403` | `/problemas/operacao-nao-permitida` |
| Acesso a recurso de outro usuário | `403` | `/problemas/acesso-negado` |
| Corpo malformado / campos inválidos / id fora do formato | `400` | `/problemas/requisicao-invalida` |
| Credenciais inválidas | `401` | `/problemas/falha-autenticacao` |
| Requisição sem autenticação | `401` | `/problemas/nao-autenticado` |
| Erro não previsto | `500` | `/problemas/erro-interno` |

Respostas de erro **não repassam** mensagens internas: a exceção original é registrada em
log e o cliente recebe uma descrição genérica.

### 6.5 Documentação e monitoramento

- **Swagger UI:** `/swagger-ui.html` — permite autenticar com o JWT obtido no login (botão
  *Authorize*) e executar os endpoints protegidos pela interface.
- **OpenAPI JSON:** `/v3/api-docs`
- **Actuator:** `/actuator/health`, `/actuator/info` (públicos) e `/actuator/metrics`
  (autenticado).

---

## 7. Conformidade com os Requisitos do Enunciado

Auditoria item a item do enunciado contra o código efetivamente entregue.

### 7.1 Requisitos funcionais

| # | Requisito | Status | Evidência |
| --- | --- | --- | --- |
| 1 | Usuário com id, nome, endereço e e-mail | ✅ | Entidade `User` + `Address`; tabelas `users` e `addresses` |
| 2 | Inclusão de usuário | ✅ | `POST /api/v1/users` |
| 3 | Atualização de usuário | ✅ | `PUT /api/v1/users/{id}` |
| 4 | Consulta por nome (`?name=`) | ✅ | `GET /api/v1/users?name=Joao` — busca parcial, sem diferenciar maiúsculas |
| 5 | Exclusão de usuário | ✅ | `DELETE /api/v1/users/{id}` |
| 6 | E-mail único | ✅ | Constraint `UNIQUE` em `users.email` **e** verificação no `UserService`, que devolve `409` antes de chegar ao banco |
| 7 | Login / autenticação | ✅ | `POST /api/v1/auth/login`, com emissão de JWT |
| 8 | Alteração de senha | ✅ | `PATCH /api/v1/users/{id}/password` |
| 9 | Endereço como entidade ou atributos | ✅ | Optou-se por **entidade própria** (`addresses`), relação 1:N |
| 10 | Tipos de usuário: Cliente e Dono do Restaurante | ✅ | `ROLE_CUSTOMER` e `ROLE_OWNER` (+ `ROLE_ADMIN`) |
| 11 | Modelagem com ou sem herança | ✅ | Optou-se por **composição via papéis** — decisão registrada na seção 4.3 |
| 12 | Auditoria de criação e/ou alteração | ✅ | `created_at`, `created_by`, `last_updated_at`, `last_updated_by` em todas as tabelas (migration `V5`); `AuditorProvider` resolve o autor, com *fallback* `system` |

### 7.2 Segurança

| # | Requisito | Status | Evidência |
| --- | --- | --- | --- |
| 13 | Spring Security não é obrigatório | ✅ | Utilizado por opção, com JWT |
| 14 | Endpoint de login recebendo usuário e senha | ✅ | `POST /api/v1/auth/login` com `{ "login", "password" }` |
| 15 | Senha não armazenada em texto aberto | ✅ | Hash **BCrypt**; a senha nunca aparece em nenhuma resposta da API |
| 16 | Endpoint de troca de senha com nova senha e confirmação | ✅ | `PATCH /api/v1/users/{id}/password` com `{ "currentPassword", "newPassword", "confirmPassword" }` |

**Divergências deliberadas em relação aos exemplos do enunciado** — o enunciado ilustra
`POST /login`, `/usuarios?name=Joao` e `POST /usuario/{usuarioId}/password`. A implementação
adota:

| Enunciado (exemplo) | Implementado | Motivo |
| --- | --- | --- |
| `POST /login` | `POST /api/v1/auth/login` | Versionamento da API e agrupamento do contexto de autenticação |
| `/usuarios?name=Joao` | `/api/v1/users?name=Joao` | Recursos nomeados em inglês, coerentes com o restante do código; o parâmetro `name` é o mesmo |
| `POST /usuario/{id}/password` | `PATCH /api/v1/users/{id}/password` | `PATCH` é o verbo semanticamente correto para alteração parcial de um recurso existente |
| `{ novaSenha, confirmacaoSenha }` | `{ currentPassword, newPassword, confirmPassword }` | Exige também a **senha atual** — regra mais restritiva que a pedida, impedindo troca de senha por token roubado |

O contrato é equivalente ao pedido e, na troca de senha, mais rigoroso. Recomenda-se
confirmar com a banca se a nomenclatura literal do enunciado é obrigatória.

### 7.3 Infraestrutura e entregáveis

| # | Requisito | Status | Evidência |
| --- | --- | --- | --- |
| 17 | Dockerfile multi-stage | ✅ | Estágio 1 `maven:3.9-eclipse-temurin-21` gera o `.jar`; estágio 2 `eclipse-temurin:21-jre-alpine` executa via JVM |
| 18 | Docker Compose com serviço de banco | ✅ | Serviço `db` (`postgres:16-alpine`) com volume e healthcheck |
| 19 | Docker Compose executando a imagem da aplicação | ✅ | Serviço `app` com `build: .`, dependente do banco saudável |
| 20 | Documentação com Swagger | ✅ | springdoc-openapi; `/swagger-ui.html` e `/v3/api-docs`, com esquema Bearer JWT |
| 21 | **Exemplo de sucesso e de falha para cada rota** | ⚠️ **Parcial** | Ver nota abaixo |
| 22 | Collection do Postman exportada em JSON | ✅ | `postman/Restaurantes.postman_collection.json` — **47 requisições** |
| 23 | Collection anexada em diretório do projeto | ✅ | `fase-01/camadas/restaurantes/postman/` |
| 24 | Relatório técnico em PDF | ✅ | Este documento |
| 25 | Link do repositório informado | ✅ | Capa e seção 10 |
| 26 | Repositório público | ✅ | Confirmado via API do GitHub: `visibility: public` |

**Nota sobre o item 21.** O enunciado pede, na seção de Swagger, "para cada rota, informar
ao menos um exemplo de sucesso e um exemplo de falha". A especificação OpenAPI é gerada
automaticamente pelo springdoc a partir dos controllers e VOs — os schemas e as restrições
de Bean Validation aparecem, mas **não há anotações `@ApiResponse` / `@ExampleObject`
declarando explicitamente os pares sucesso/falha por rota**. O código não possui nenhuma
anotação Swagger na variante em camadas.

Esse conteúdo existe, porém, na **collection do Postman**: as 47 requisições cobrem sucesso
e falha de todas as rotas, com os 47 prints correspondentes em `postman/prints/`. Cada
cenário traz o status esperado no próprio nome — por exemplo, `Cadastro inválido — e-mail
duplicado (409)` e `Buscar por id de outro usuário — negado (403)`.

**Recomendação:** anotar os controllers com `@ApiResponse` + `@ExampleObject` para atender ao
item literalmente dentro do Swagger. É a única lacuna formal identificada nesta auditoria, e
o esforço é baixo — a collection já contém os corpos de requisição e resposta a serem
reaproveitados.

### 7.4 Cobertura da collection Postman

Distribuição dos 47 cenários, com sucesso e falha para cada rota:

| Área | Cenários | Exemplos de falha cobertos |
| --- | --- | --- |
| Autenticação | 3 | Credenciais incorretas (401), campos ausentes (400) |
| Cadastro | 5 | Campos ausentes (400), corpo malformado (400), e-mail duplicado (409), login duplicado (409), papel `ROLE_ADMIN` proibido (403) |
| Listagem e busca | 5 | Sem token (401), ordenação por coluna não permitida (ignorada) |
| Consulta por id | 6 | Recurso de outro usuário (403), inexistente (404), UUID inválido (400), sem token (401) |
| Atualização | 5 | Recurso de outro usuário (403), inexistente (404), campos inválidos (400), sem token (401) |
| Troca de senha | 4 | Senha atual incorreta (400), confirmação divergente (400), usuário alheio (403) |
| Recuperação de senha | 7 | E-mail inválido (400), token inválido (400), senhas divergentes (400), token já utilizado (400) |
| Exclusão | 4 | Usuário alheio (403), inexistente (404), sem token (401) |
| Monitoramento e OpenAPI | 5 | Métricas sem token (401) |

---

## 8. Qualidade e Testes

### 8.1 Suíte automatizada

| Projeto | Classes de produção | Classes de teste | Testes | Cobertura |
| --- | --- | --- | --- | --- |
| **Camadas** (oficial) | 54 | 36 | **252** | **100%** |
| **Hexagonal** (comparativo) | 94 | 35 | **290** | **100%** |

A cobertura é de **100% em todos os contadores do JaCoCo** — instruções, desvios (*branches*),
linhas, complexidade, métodos e classes — **sem nenhuma exclusão**, incluindo as
implementações geradas pelo MapStruct, que são testadas.

O nível não é apenas um número de relatório: o `jacoco:check` roda na fase `verify` com
mínimo de `1.00` em cada contador, e **o build falha se a cobertura cair**. Reprodução:

```bash
cd fase-01/camadas/restaurantes && mvn clean verify
```

A suíte cobre entidades e builders, VOs com Bean Validation, utilitários, mappers, os três
repositórios JDBC (com os `RowMapper` e `RowCallbackHandler` capturados e alimentados por
`ResultSet` simulado), a camada de segurança, os services, os controllers, o assembler
HATEOAS e as regras de arquitetura via ArchUnit.

### 8.2 Pontos que exigiram técnica específica

Três trechos não são alcançáveis por um teste convencional, e cada um está justificado no
próprio código de teste:

- **`PasswordResetTokenGenerator.hash`** — o `catch (NoSuchAlgorithmException)` nunca dispara
  numa JVM real, já que SHA-256 é obrigatório. A fábrica do `MessageDigest` é substituída
  para comprovar que a falha vira um erro de estado explícito, e não um token sem hash.
- **`JwtService.isTokenValid`** — a checagem própria de expiração é uma segunda linha de
  defesa: o parser do jjwt já barra o token vencido antes dela. O parser é substituído para
  exercitar esse caminho.
- **`SecurityConfig`** — regra de acesso não se verifica por chamada de método. Uma fatia
  `@WebMvcTest`, com o filtro e o *entry point* reais, confirma que o autocadastro segue
  público e que os demais endpoints respondem `401`.

### 8.3 Defeito encontrado e corrigido pelos testes

Durante a construção da suíte da variante hexagonal, os testes revelaram um defeito real:

`UserPersistenceAdapter.ordenacao` consultava o mapa de colunas permitidas com a propriedade
de ordenação vinda do `PageQuery`. O mapa é um `Map.of`, que **lança `NullPointerException`
para chave nula** — e `sortBy` nulo é um estado que o próprio código produz de propósito
(`PageMapper.toQuery` quando o `Pageable` não traz ordenação, e `PageQuery.of`, que passa
`null` explicitamente). Em produção só não estourava porque o controller injeta
`@PageableDefault(sort = "name")`; qualquer chamada com `Pageable` sem ordenação retornaria
HTTP 500.

A guarda passou a vir antes da consulta ao mapa, e o caso está coberto por teste. É a
demonstração prática do valor da meta de cobertura: o defeito estava em um ramo que nenhum
teste anterior alcançava.

### 8.4 Verificação de arquitetura

Além dos testes de comportamento, o build verifica a arquitetura:

- **Camadas:** 5 regras de ArchUnit — nenhum acesso de controller a repository, entidades
  independentes de VOs e controllers, contrato do pacote `repository` em interfaces.
- **Hexagonal:** 9 regras verificando a regra de dependência do hexágono — o núcleo
  (`domain` e `application`) não conhece framework algum.

---

## 9. Considerações Finais

O levantamento de requisitos por Event Storming ajudou a identificar os processos do sistema
e a organizar os elementos do domínio. Os conceitos de DDD orientaram a definição das
responsabilidades das entidades e a organização da arquitetura.

Os principais desafios estiveram na autenticação com JWT, no controle de acesso por perfil e
por posse do recurso, na substituição do ORM por SQL explícito — que tornou visível o que o
framework fazia implicitamente — e em alcançar 100% de cobertura sem recorrer a exclusões.

O resultado é uma API REST segura, organizada e verificada de ponta a ponta: 252 testes com
cobertura total travada no build, regras de arquitetura automatizadas e uma collection com
47 cenários de sucesso e falha. A implementação paralela em arquitetura hexagonal, sobre a
mesma stack, permitiu comparar as duas abordagens com a arquitetura como única variável.

**Pendências identificadas nesta auditoria:**

1. Anotar os controllers com `@ApiResponse` / `@ExampleObject` para atender literalmente ao
   item de exemplos por rota no Swagger (seção 7.3).
2. Atualizar a contagem de testes citada no `fase-01/README.md`, que ainda registra 23 e 80
   testes — números anteriores à suíte atual de 252 e 290.

---

## 10. Artefatos

| Artefato | Descrição | Localização |
| --- | --- | --- |
| Repositório | Monorepo público com todas as fases | https://github.com/felipe11dias/postech-restaurantes-app |
| Código da aplicação | Entregável oficial em arquitetura em camadas | `fase-01/camadas/restaurantes/` |
| Variante hexagonal | Implementação comparativa em Ports & Adapters | `fase-01/hexagonal/restaurantes/` |
| Relatório técnico | Este documento | `fase-01/relatorios/camadas/` |
| Relatório de engenharia | Detalhamento etapa a etapa da construção | `fase-01/relatorios/camadas/relatorio-tech-challenge-fase01-v2.md` |
| Documentação da API | Swagger/OpenAPI interativo | Aplicação em `/swagger-ui.html` |
| Coleção Postman | 47 requisições de sucesso e falha | `fase-01/camadas/restaurantes/postman/Restaurantes.postman_collection.json` |
| Prints das respostas | 47 imagens, uma por cenário | `fase-01/camadas/restaurantes/postman/prints/` |
| Scripts de banco | Migrations Flyway `V1` a `V6` | `fase-01/camadas/restaurantes/src/main/resources/db/migration/` |
| Docker | Imagem multi-stage e orquestração | `fase-01/camadas/restaurantes/Dockerfile` e `docker-compose.yml` |
| Relatório de cobertura | HTML gerado pelo JaCoCo | `target/site/jacoco/index.html` após `mvn verify` |

---

## 11. Referências

- EVANS, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Boston: Addison-Wesley, 2003.
- FOWLER, Martin. *Patterns of Enterprise Application Architecture*. Boston: Addison-Wesley, 2002.
- MARTIN, Robert C. *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Boston: Prentice Hall, 2017.
- Spring. *Spring Boot Documentation*. Disponível em: https://docs.spring.io/spring-boot/index.html. Acesso em: 28 ago. 2026.
- Spring. *Spring Security Reference*. Disponível em: https://docs.spring.io/spring-security/reference/. Acesso em: 28 ago. 2026.
- PostgreSQL Global Development Group. *PostgreSQL Documentation*. Disponível em: https://www.postgresql.org/docs/. Acesso em: 28 ago. 2026.
- Flyway. *Flyway Documentation*. Disponível em: https://documentation.red-gate.com/flyway. Acesso em: 28 ago. 2026.
- Docker Inc. *Docker Documentation*. Disponível em: https://docs.docker.com/. Acesso em: 28 ago. 2026.
- OpenAPI Initiative. *OpenAPI Specification*. Disponível em: https://swagger.io/specification/. Acesso em: 28 ago. 2026.
- IETF. *RFC 7807 — Problem Details for HTTP APIs*. Disponível em: https://datatracker.ietf.org/doc/html/rfc7807. Acesso em: 28 ago. 2026.
- EclEmma. *JaCoCo Java Code Coverage Library*. Disponível em: https://www.jacoco.org/jacoco/. Acesso em: 28 ago. 2026.
- TNG Technology Consulting. *ArchUnit User Guide*. Disponível em: https://www.archunit.org/userguide/html/000_Index.html. Acesso em: 28 ago. 2026.

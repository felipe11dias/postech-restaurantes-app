# Sistema de Gestão de Restaurantes — Tech Challenge Fase 1 (Arquitetura Hexagonal)

Implementação da **Fase 1** do Tech Challenge sob **Arquitetura Hexagonal (Ports &
Adapters)**, como variante comparativa da versão em camadas
([`../../camadas/restaurantes/`](../../camadas/restaurantes/)). Mesmo sistema de gestão de
usuários (donos de restaurante e clientes), mesmos requisitos, mesma stack — **o que muda é
só a arquitetura**.

> **Status: completo.** 290 testes automatizados passando (100% de cobertura) e a aplicação
> verificada end-to-end sobre Docker. Ver o [relatório técnico](../../relatorios/hexagonal/relatorio-fase01-hexagonal.md)
> e o [plano de sprints](../../relatorios/hexagonal/plano-de-sprints.md).

## Por que a stack é idêntica à da variante em camadas

Java 21, JDBC puro, sem Lombok, PostgreSQL, Flyway, JWT e ids em UUID — exatamente como a
outra variante. Se as duas tivessem stacks diferentes, qualquer diferença observada (em
legibilidade, esforço de teste, acoplamento) poderia ser atribuída à troca de tecnologia em
vez do desenho arquitetural. Com a stack fixa, **a única variável é a arquitetura**.

A exceção é o MapStruct, que a variante em camadas usa no mapeamento VO ↔ entidade e que aqui
não entra: a tradução DTO ↔ command/view ↔ domínio é escrita à mão, para que nenhuma geração
de código atravesse a fronteira do núcleo.

## Stack

- Java 21 · Spring Boot 3.5.x
- Spring Web (adapter de entrada REST) · Spring JDBC + Flyway (adapter de persistência)
- Spring Security · jjwt (adapters de segurança JWT/BCrypt) · Spring Mail
- Bean Validation · springdoc-openapi (Swagger) · Spring HATEOAS · Actuator
- JUnit 5 · Mockito · ArchUnit (verificação da regra de dependência do hexágono)

## Estrutura de pacotes (por arquitetura, não por camada técnica)

```
com.postech.restaurantes
├── RestaurantesApplication.java
├── domain/                        # NÚCLEO — Java puro, zero dependências externas
│   ├── model/                     # User, Address, Role, PasswordResetToken
│   │   └── shared/                # Email, ZipCode (Value Objects)
│   ├── exception/                 # DomainException e descendentes
│   └── util/
├── application/                   # também sem framework
│   ├── pagination/                # PageQuery, PageResult (paginação própria)
│   ├── port/in/                   # casos de uso + commands + views
│   ├── port/out/                  # contratos de infraestrutura
│   └── service/                   # implementações dos casos de uso
├── adapter/
│   ├── in/web/                    # controllers, DTOs v1, ProblemDetail, HATEOAS, filtro JWT
│   └── out/
│       ├── persistence/           # JDBC + transação
│       ├── security/              # BCrypt, JWT, token de reset, auditor
│       └── mail/                  # SMTP
└── config/                        # raiz de composição (@Bean dos casos de uso, Security, OpenAPI)
```

**Regra de dependência:** todas as setas apontam **para dentro**. Os adapters dependem dos
ports; o núcleo não conhece nenhum adapter. Isso não é convenção — é
[verificado por ArchUnit](src/test/java/com/postech/restaurantes/architecture/HexagonalArchitectureTest.java)
a cada build.

**O núcleo não tem uma única anotação de framework.** Os serviços de aplicação são
instanciados por `@Bean` na [`UseCaseConfiguration`](src/main/java/com/postech/restaurantes/config/UseCaseConfiguration.java),
não por `@Service`. O custo é manter essa configuração; o ganho é que `domain` e
`application` compilam e rodam sem Spring no classpath — o que torna os testes de caso de
uso testes de unidade de verdade.

## Como executar

```bash
cp .env.example .env
```

```bash
docker compose up --build
```

Aplicação em `http://localhost:8080` · Swagger em `http://localhost:8080/swagger-ui.html`.

As duas variantes usam as portas `8080`/`5432` — **rode uma de cada vez**. Os containers e o
volume desta têm sufixo `-hex` para não conflitar.

Para encerrar:

```bash
docker compose down
```

(use `docker compose down -v` para apagar também os dados)

## Usuários de demonstração

Criados pela migration `V2`. **Apenas para demonstração.**

| Login | Senha | Papel |
|-------|-------|-------|
| `dono.restaurante` | `dono12345` | ROLE_OWNER |
| `cliente.demo` | `cliente12345` | ROLE_CUSTOMER |
| `admin.demo` | `admin12345` | ROLE_ADMIN |

O papel `ROLE_ADMIN` **não** pode ser obtido pelo autocadastro público — só por seed.

## Fluxo de autenticação

1. `POST /api/v1/auth/login` com `login` e `password` → devolve o token.
2. Enviar `Authorization: Bearer <token>` nas demais requisições.

Endpoints públicos: login, recuperação de senha, autocadastro (`POST /api/v1/users`),
Swagger e health. O restante exige token.

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/users` | Cadastro (público) |
| `GET` | `/api/v1/users/{id}` | Consulta por id |
| `GET` | `/api/v1/users?name=` | Lista/busca por nome (paginada) |
| `PUT` | `/api/v1/users/{id}` | Atualiza dados (não altera senha) |
| `PATCH` | `/api/v1/users/{id}/password` | Troca de senha (endpoint exclusivo) |
| `DELETE` | `/api/v1/users/{id}` | Exclusão |
| `POST` | `/api/v1/auth/login` | Validação de login |
| `POST` | `/api/v1/auth/forgot-password` | Solicita token de redefinição |
| `POST` | `/api/v1/auth/reset-password` | Redefine a senha com o token |

Autorização: um usuário só acessa o próprio registro; `ROLE_ADMIN` acessa qualquer um.

### Recuperação de senha sem servidor SMTP

O token é enviado por e-mail e **nunca** retornado pela API. Sem um SMTP configurado, o envio
falha silenciosamente (de propósito — ver abaixo), mas o token é registrado em log:

```bash
docker logs restaurantes-hex-app | grep "Token de redefinição"
```

A falha de envio não vira erro HTTP porque a resposta precisa ser idêntica para e-mail
cadastrado e não cadastrado. Se uma queda de SMTP produzisse 500 só quando a conta existe, a
diferença de status revelaria quais e-mails estão cadastrados.

## Coleção Postman

`postman/Restaurantes-Hexagonal.postman_collection.json` — importe e use a variável
`{{baseUrl}}`. O login salva o token automaticamente em `{{token}}`. A coleção cobre casos de
sucesso e de erro (duplicidade, validação, autorização, senha incorreta, token reusado).

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_NAME` | `restaurantes-hex` | Nome do banco |
| `DB_USER` / `DB_PASSWORD` | `postgres` | Credenciais do banco |
| `DB_HOST` / `DB_PORT` | `localhost` (`db` no compose) / `5432` | Endereço do banco |
| `JWT_SECRET` | *(exemplo)* | Segredo do JWT (mín. 256 bits) |
| `JWT_EXPIRATION` | `3600000` | Expiração do token (ms) |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | SMTP de envio |
| `MAIL_FROM` | `no-reply@restaurantes.postech` | Remetente |
| `MAIL_RESET_TOKEN_EXPIRATION_MINUTES` | `30` | Validade do token de redefinição |

## Testes

```bash
mvn verify
```

290 testes: domínio (sem Spring), casos de uso (mocks dos ports) e nove regras de ArchUnit
que protegem a regra de dependência do hexágono. Nenhum sobe contexto Spring ou banco.
A cobertura é de 100% em todos os contadores do JaCoCo e o `jacoco:check` (fase `verify`)
falha o build se ela cair.

# Sistema de Gestão de Restaurantes — Tech Challenge Fase 1 (Arquitetura Hexagonal)

Implementação da **Fase 1** do Tech Challenge sob **Arquitetura Hexagonal (Ports &
Adapters)**, como variante de estudo da versão em camadas
([`../../camadas/restaurantes/`](../../camadas/restaurantes/)). O objetivo é o mesmo
sistema de gestão de usuários (donos de restaurante e clientes), mas com o domínio isolado
de framework.

> **Status: Etapa 1 — Setup do Projeto e Estrutura Hexagonal (scaffold).**
> Este projeto contém, por enquanto, apenas o esqueleto: build, infraestrutura e a árvore
> de pacotes do hexágono. Domínio, casos de uso, adapters e migrations chegam nas etapas
> seguintes. Ver o [relatório técnico hexagonal](../../relatorios/hexagonal/relatorio-fase01-hexagonal.md).

## Stack

- Java 21 · Spring Boot 3.5.x
- Spring Web (adapter de entrada REST) · Spring Data JPA + Flyway (adapter de persistência)
- Spring Security · jjwt (adapter de segurança JWT/BCrypt)
- Bean Validation · springdoc-openapi (Swagger) · Spring Boot Actuator
- MapStruct (mapeamento domínio ↔ VO web ↔ entidade JPA) · Lombok
- JUnit 5 · Mockito · ArchUnit (verificação da regra de dependência do hexágono)

## Estrutura de pacotes (por arquitetura, não por camada técnica)

```
com.postech.restaurantes
├── RestaurantesApplication.java   # @SpringBootApplication (bootstrap)
├── domain/                        # NÚCLEO — sem dependências de framework
│   ├── model/                     # entidades de domínio puras (POJOs)
│   ├── vo/                        # Value Objects (Email, ZipCode...)
│   └── exception/                 # exceções de domínio
├── application/
│   ├── port/
│   │   ├── in/                    # Input Ports: casos de uso (interfaces)
│   │   └── out/                   # Output Ports: contratos de infraestrutura (interfaces)
│   └── service/                   # serviços de aplicação (implementam os input ports)
└── adapter/
    ├── in/
    │   └── web/                   # controllers REST, VOs request/response, ProblemDetail, Swagger
    └── out/
        ├── persistence/           # entidades JPA, repos Spring Data, adapter dos output ports
        └── security/              # BCrypt e JWT como adapters dos output ports
```

**Regra de dependência:** todas as setas apontam **para dentro**, em direção ao domínio.
Os adapters dependem dos ports (interfaces da aplicação); o domínio não conhece nenhum
adapter. As pastas ainda vazias trazem um `.gitkeep` para versionar a estrutura.

## Roadmap das etapas (relatório hexagonal)

| Etapa | Camada hexagonal | Status |
|-------|------------------|--------|
| 1 · Setup do projeto e estrutura hexagonal | — | ✅ (este scaffold) |
| 2 · Modelagem do domínio | Domínio | ⏳ |
| 3–5 · Ports de entrada/saída e serviços | Aplicação | ⏳ |
| 6 · Adapter de persistência + migrations | Adapter de saída | ⏳ |
| 7 · Adapter web REST | Adapter de entrada | ⏳ |
| 8 · Adapter de segurança (JWT/BCrypt) | Adapter de saída | ⏳ |
| 9–10 · ProblemDetail + Swagger | Adapter de entrada | ⏳ |
| 11 · Docker Compose | — | ✅ (infra pronta) |
| 12 · Testes (domínio, use cases, ArchUnit) | Transversal | ⏳ |

## Como executar (infra já disponível)

> Ainda sem endpoints — o scaffold sobe vazio. Útil para validar a infraestrutura.

```bash
# 1. (Opcional) criar seu .env a partir do exemplo
cp .env.example .env

# 2. Subir aplicação + banco
docker compose up --build
```

Aplicação em `http://localhost:8080`. Como as duas variantes (camadas e hexagonal) usam as
portas `8080`/`5432`, **rode uma de cada vez**; os containers e o volume desta variante têm
sufixo `-hex` para não conflitar.

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_NAME` | `restaurantes-hex` | Nome do banco |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `DB_HOST` | `localhost` / `db` (compose) | Host do banco |
| `DB_PORT` | `5432` | Porta do banco |
| `JWT_SECRET` | *(exemplo)* | Segredo do JWT (mín. 256 bits) |
| `JWT_EXPIRATION` | `3600000` | Expiração do token (ms) |

## Testes

```bash
mvn test
```

A verificação de arquitetura (ArchUnit) que protege a regra de dependência do hexágono
entra na Etapa 12.

# Pós-Tech — Fase 01 · Arquitetura e Desenvolvimento Java

Pasta de versionamento e organização dos entregáveis da **Fase 1** da pós-graduação.
Parte do monorepo [`postech`](../README.md), que reúne todas as fases do curso.

## Tech Challenge — Sistema de Gestão de Restaurantes

Backend em Spring Boot para gestão de usuários (donos de restaurante e clientes),
com autenticação JWT, banco PostgreSQL e orquestração via Docker Compose.

Como desafio de estudo, a mesma Fase 1 foi **bifurcada em duas arquiteturas**, permitindo
comparar as abordagens sobre o mesmo conjunto de requisitos:

| Variante | Pasta | Arquitetura | Status |
|----------|-------|-------------|--------|
| **Camadas** — entregável oficial | [`camadas/restaurantes/`](camadas/restaurantes/) | Em camadas (`controller` → `service` → `repository`), SOLID + Clean Code | ✅ completo |
| **Hexagonal** — variante comparativa | [`hexagonal/restaurantes/`](hexagonal/restaurantes/) | Ports & Adapters (domínio isolado de framework) | ✅ completo |

### A comparação é controlada

As duas variantes usam **a mesma stack**: Java 21, JDBC puro com `JdbcTemplate`, sem Lombok,
PostgreSQL, Flyway, JWT, ids em UUID. Isso é deliberado — se as stacks diferissem, qualquer
diferença observada entre elas poderia ser atribuída à tecnologia em vez do desenho
arquitetural. **Com a stack fixa, a única variável é a arquitetura.**

A única biblioteca que não se repete é o MapStruct: a variante em camadas o usa no mapeamento
VO ↔ entidade; o hexagonal escreve à mão a tradução DTO ↔ command/view ↔ domínio, para que
nenhuma geração de código atravesse a fronteira do núcleo.

Diferenças que valem a leitura lado a lado:

| Aspecto | Camadas | Hexagonal |
|---------|---------|-----------|
| Regras de negócio | entidades anêmicas + lógica no `Service` | entidades com invariantes; serviços só orquestram |
| Dependências do núcleo | `@Service`, `@Transactional`, `Pageable` do Spring | nenhuma anotação; paginação e transação como ports |
| Autenticação | delegada ao `AuthenticationManager` | regra explícita no caso de uso |
| Representações por requisição | 2 (DTO ↔ entidade) | 3 (DTO ↔ command/view ↔ domínio) |
| Testar uma regra | mock de repositório Spring | mock de interfaces próprias, sem framework |
| Arquitetura verificada | ArchUnit — 5 regras de camadas | ArchUnit — 9 regras da regra de dependência |
| Suíte automatizada | 252 testes · 100% cobertura | 290 testes · 100% cobertura |

O custo do hexagonal é real e está registrado no relatório: mais uma camada de mapeamento e
uma raiz de composição para manter. O ganho é um núcleo que compila e roda sem framework.

Os relatórios técnicos (entregável oficial de cada variante) ficam em
[`relatorios/`](relatorios/), com um subdiretório por arquitetura.

## Estrutura desta pasta

```
fase-01/
├── README.md                     # este índice
├── camadas/
│   └── restaurantes/             # aplicação Spring Boot (em camadas)
│       ├── pom.xml, Dockerfile, docker-compose.yml, .env.example
│       ├── postman/              # coleção de testes
│       └── src/...
├── hexagonal/
│   └── restaurantes/             # aplicação Spring Boot (Ports & Adapters)
│       ├── pom.xml, Dockerfile, docker-compose.yml, .env.example
│       ├── postman/              # coleção de testes
│       └── src/...
└── relatorios/                   # entregáveis oficiais
    ├── camadas/
    │   ├── Tech Challenge - Entregável.pdf        # relatório de entrega (documento final)
    │   ├── relatorio-tech-challenge-fase01-v2.md  # relatório detalhado da construção
    │   ├── relatorio-tech-challenge-fase01-v1.1.md / .pdf
    │   └── CHANGELOG.md
    └── hexagonal/
        ├── relatorio-fase01-hexagonal.md   # relatório técnico (v2.0)
        ├── plano-de-sprints.md             # roteiro de execução, sprint a sprint
        └── CHANGELOG.md                    # histórico de versões do relatório
```

## Entregáveis da fase

| Entregável | Camadas | Hexagonal |
|------------|---------|-----------|
| Relatório técnico (Markdown→PDF) | ✅ `relatorios/camadas/` | ✅ `relatorios/hexagonal/` |
| Código-fonte | ✅ `camadas/restaurantes/` | ✅ `hexagonal/restaurantes/` |
| README do projeto | ✅ | ✅ |
| Documentação Swagger | ✅ gerada pela app | ✅ gerada pela app |
| Coleção Postman (JSON) | ✅ `camadas/restaurantes/postman/` | ✅ `hexagonal/restaurantes/postman/` |
| Testes automatizados | ✅ 252 testes · 100% cobertura | ✅ 290 testes · 100% cobertura |

## Executando

As duas variantes usam as portas `8080`/`5432` — **rode uma de cada vez**. Os containers e o
volume da variante hexagonal têm sufixo `-hex` para não conflitar.

```bash
cd fase-01/hexagonal/restaurantes && docker compose up --build
```

## Versionamento

- Cada relatório segue versionamento próprio, com `CHANGELOG.md` na pasta da variante
  ([camadas](relatorios/camadas/CHANGELOG.md) · [hexagonal](relatorios/hexagonal/CHANGELOG.md)).
- Os contratos da API (DTOs de request/response) são versionados no código sob `v1`.

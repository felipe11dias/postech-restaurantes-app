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
| **Camadas** | [`camadas/restaurantes/`](camadas/restaurantes/) | Em camadas (`controller` → `service` → `repository`), SOLID + Clean Code | ✅ completo |
| **Hexagonal** | [`hexagonal/restaurantes/`](hexagonal/restaurantes/) | Ports & Adapters (domínio isolado de framework) | 🔄 Etapa 1 (scaffold) |

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
│   └── restaurantes/             # aplicação Spring Boot (hexagonal) — scaffold Etapa 1
│       ├── pom.xml, Dockerfile, docker-compose.yml, .env.example
│       └── src/...
└── relatorios/                   # entregáveis oficiais (viram PDF na entrega)
    ├── camadas/
    │   ├── relatorio-tech-challenge-fase01-v1.1.md
    │   └── CHANGELOG.md           # histórico de versões do relatório
    └── hexagonal/
        └── relatorio-fase01-hexagonal.md
```

## Entregáveis da fase

| Entregável | Camadas | Hexagonal |
|------------|---------|-----------|
| Relatório técnico (Markdown→PDF) | ✅ `relatorios/camadas/` | 🔄 `relatorios/hexagonal/` |
| Código-fonte | ✅ `camadas/restaurantes/` | 🔄 `hexagonal/restaurantes/` (scaffold) |
| README do projeto | ✅ | ✅ |
| Documentação Swagger | ✅ gerada pela app | ⏳ Etapa 10 |
| Coleção Postman (JSON) | ✅ `camadas/restaurantes/postman/` | ⏳ Etapa 13 |

## Versionamento

- Cada relatório segue versionamento próprio (ver o `CHANGELOG.md` da variante camadas).
- Os contratos da API (VOs de request/response) são versionados no código sob `v1`.

# Pós-Tech — Arquitetura e Desenvolvimento Java

Monorepo com os entregáveis das fases da pós-graduação **Pós-Tech — Arquitetura e
Desenvolvimento Java**. Cada fase vive em sua própria pasta, versionada em conjunto neste
repositório único.

## Fases

| Fase | Pasta | Tema | Status |
|------|-------|------|--------|
| 01 | [`fase-01/`](fase-01/) | Tech Challenge — Sistema de Gestão de Restaurantes (Spring Boot) | ✅ concluída |
| 02 | `fase-02/` | *(a ser adicionada)* | ⏳ |

> Novas fases entram como pastas irmãs (`fase-02/`, `fase-03/`...), mantendo o histórico
> de todas as entregas em um só lugar.

## Fase 01 — Tech Challenge

Backend em Spring Boot para gestão de usuários (donos de restaurante e clientes), com
autenticação JWT, banco PostgreSQL e orquestração via Docker Compose.

Como desafio de estudo, a **mesma Fase 1** é implementada em **duas arquiteturas** lado a
lado, e **as duas estão concluídas**:

| Variante | Código-fonte | Relatório | Status |
|----------|--------------|-----------|--------|
| **Em camadas** (SOLID + Clean Code) — entregável oficial | [`fase-01/camadas/restaurantes/`](fase-01/camadas/restaurantes/) | [`fase-01/relatorios/camadas/`](fase-01/relatorios/camadas/) | ✅ completo — 252 testes · 100% cobertura |
| **Hexagonal** (Ports & Adapters) — variante comparativa | [`fase-01/hexagonal/restaurantes/`](fase-01/hexagonal/restaurantes/) | [`fase-01/relatorios/hexagonal/`](fase-01/relatorios/hexagonal/) | ✅ completo — 290 testes · 100% cobertura |

As duas variantes cobrem os mesmos requisitos sobre **a mesma stack** (Java 21, JDBC puro
com `JdbcTemplate`, sem Lombok, PostgreSQL, Flyway, JWT, ids em UUID). A stack é fixa de
propósito: assim **a única variável entre elas é a arquitetura**. A leitura comparativa —
inclusive do que o hexagonal cobra em troca — está em [`fase-01/README.md`](fase-01/README.md).

- **Como executar / detalhes:** ver [`fase-01/README.md`](fase-01/README.md) e o README de
  cada projeto. As duas usam as portas `8080`/`5432` — rode uma de cada vez.

## Estrutura do repositório

```
postech/
├── README.md                     # este índice
├── .gitignore
└── fase-01/
    ├── README.md                 # índice da fase e comparação entre as variantes
    ├── camadas/
    │   └── restaurantes/         # aplicação Spring Boot (arquitetura em camadas)
    │       └── postman/          # coleção de testes
    ├── hexagonal/
    │   └── restaurantes/         # aplicação Spring Boot (Ports & Adapters)
    │       └── postman/          # coleção de testes
    └── relatorios/               # entregáveis oficiais (Markdown → PDF)
        ├── camadas/              # relatório + CHANGELOG
        └── hexagonal/            # relatório + CHANGELOG + plano de sprints
```

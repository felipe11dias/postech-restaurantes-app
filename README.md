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
lado:

| Variante | Código-fonte | Relatório | Status |
|----------|--------------|-----------|--------|
| **Em camadas** (SOLID + Clean Code) — entregável oficial | [`fase-01/camadas/restaurantes/`](fase-01/camadas/restaurantes/) | [`fase-01/relatorios/camadas/`](fase-01/relatorios/camadas/) | ✅ completo |
| **Hexagonal** (Ports & Adapters) — variante comparativa | [`fase-01/hexagonal/restaurantes/`](fase-01/hexagonal/restaurantes/) | [`fase-01/relatorios/hexagonal/`](fase-01/relatorios/hexagonal/) | 🔄 Etapa 1 (scaffold) |

- **Como executar / detalhes:** ver [`fase-01/README.md`](fase-01/README.md) e o README de
  cada projeto.

## Estrutura do repositório

```
postech/
├── README.md                     # este índice
├── .gitignore
└── fase-01/
    ├── README.md                 # índice da fase
    ├── camadas/
    │   └── restaurantes/         # aplicação Spring Boot (arquitetura em camadas)
    ├── hexagonal/
    │   └── restaurantes/         # aplicação Spring Boot (arquitetura hexagonal)
    └── relatorios/               # entregáveis oficiais (Markdown → PDF)
        ├── camadas/
        └── hexagonal/
```

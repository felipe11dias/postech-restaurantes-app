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

- **Código-fonte:** [`fase-01/restaurantes/`](fase-01/restaurantes/)
- **Relatório técnico (entregável oficial):** [`fase-01/relatorio/`](fase-01/relatorio/)
- **Como executar / detalhes:** ver [`fase-01/README.md`](fase-01/README.md) e o
  [README do projeto](fase-01/restaurantes/README.md).

## Estrutura do repositório

```
postech/
├── README.md                 # este índice
├── .gitignore
└── fase-01/
    ├── README.md             # índice da fase
    ├── relatorio/            # entregável oficial (Markdown → PDF)
    └── restaurantes/         # aplicação Spring Boot
```

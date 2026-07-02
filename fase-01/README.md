# Pós-Tech — Fase 01 · Arquitetura e Desenvolvimento Java

Pasta de versionamento e organização dos entregáveis da **Fase 1** da pós-graduação.
Parte do monorepo [`postech`](../README.md), que reúne todas as fases do curso.

## Tech Challenge — Sistema de Gestão de Restaurantes

Backend em Spring Boot para gestão de usuários (donos de restaurante e clientes),
com autenticação JWT, banco PostgreSQL e orquestração via Docker Compose.

## Estrutura desta pasta

```
fase-01/
├── README.md                  # este índice
├── relatorio/                 # entregável oficial (vira PDF na entrega)
│   ├── relatorio-tech-challenge-fase01-v1.1.md
│   └── CHANGELOG.md           # histórico de versões do relatório
└── restaurantes/              # código da aplicação
    ├── pom.xml
    ├── Dockerfile
    ├── docker-compose.yml
    ├── .env.example
    └── src/...
```

## Entregáveis da fase

| Entregável | Local | Status |
|------------|-------|--------|
| Relatório técnico (Markdown→PDF) | `relatorio/` | ✅ pronto (gerar PDF na entrega) |
| Código-fonte | `restaurantes/` | ✅ completo |
| README do projeto | `restaurantes/README.md` | ✅ pronto |
| Documentação Swagger | gerada pela app | ✅ configurada |
| Coleção Postman (JSON) | `restaurantes/postman/` | ✅ pronta |

## Versionamento

- O relatório segue versionamento próprio (ver `relatorio/CHANGELOG.md`), iniciando em v1.0.
- Os contratos da API (VOs de request/response) são versionados no código sob `vo/v1`.

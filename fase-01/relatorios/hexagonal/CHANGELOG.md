# Changelog — Relatório Técnico Hexagonal (Tech Challenge Fase 1)

Histórico de versões do relatório da variante em **Arquitetura Hexagonal (Ports &
Adapters)**. Formato: versão · mudanças. O equivalente da variante em camadas está em
[`../camadas/CHANGELOG.md`](../camadas/CHANGELOG.md).

## v2.0 — entrega da variante hexagonal

Primeira versão que descreve **o que foi efetivamente construído e verificado**, e não um
projeto planejado. Todas as nove sprints concluídas; ver
[`plano-de-sprints.md`](plano-de-sprints.md).

- **Correção de fundo:** a v1.0 marcava as treze etapas como concluídas quando existiam
  apenas o esqueleto de pacotes e um modelo de domínio parcial. O Sumário de Progresso passa
  a refletir o código realmente entregue, sprint a sprint.
- **Stack alinhada à variante em camadas:** Spring Data JPA, Lombok e ids `BIGINT` —
  decisões da v1.0 que a variante em camadas já havia revertido — saem em favor de JDBC puro
  (`JdbcTemplate`), Java sem geração de código e ids em `UUID`. O MapStruct também sai: a
  tradução DTO ↔ command/view ↔ domínio é escrita à mão, para que nenhuma geração de código
  atravesse a fronteira do núcleo. Com a stack fixa nas duas variantes, a única variável da
  comparação é a arquitetura.
- **Domínio com invariantes próprias:** `User`, `Address`, `Role`, `PasswordResetToken` e os
  Value Objects `Email`/`ZipCode`, sem setters públicos e sem uma única anotação de
  framework. Timestamps de criação e última alteração nascem na entidade, não no ORM nem no
  serviço.
- **Ports segregados por intenção:** oito input ports (um por caso de uso), com *commands* e
  *views* como fronteira, mais os tipos próprios de paginação `PageQuery`/`PageResult` — o
  núcleo não importa `Pageable`. Catorze output ports, incluindo `TransactionPort` e
  `AuditorPort`.
- **Serviços de aplicação sem framework:** instanciados por `@Bean` na
  `UseCaseConfiguration` (raiz de composição), não por `@Service`. `domain` e `application`
  compilam e rodam sem Spring no classpath.
- **Adapters de saída:** persistência em JDBC com migrations Flyway (`V1` schema + papéis,
  `V2` usuários de demonstração), segurança (BCrypt, JWT, gerador de token de redefinição,
  auditor) e e-mail SMTP.
- **Adapter de entrada web:** controllers REST versionados (`/api/v1`), DTOs `record`
  validados, ProblemDetail (RFC 7807), HATEOAS, filtro JWT, autorização por dono do recurso
  e Swagger.
- **Recuperação de senha** em paridade com a variante em camadas: token enviado por e-mail e
  nunca devolvido pela API; falha de SMTP não vira erro HTTP, para que a resposta seja
  idêntica com e-mail cadastrado ou não.
- **Testes:** 80 testes — domínio (sem Spring), casos de uso (mocks dos ports) e **nove
  regras de ArchUnit** que verificam a regra de dependência do hexágono a cada build.
  Nenhum sobe contexto Spring ou banco.
- **Entregáveis:** coleção Postman própria
  (`hexagonal/restaurantes/postman/Restaurantes-Hexagonal.postman_collection.json`, cobrindo
  casos de sucesso e de erro), README do projeto reescrito e novo `plano-de-sprints.md` com
  escopo, decisões e critérios de aceite de cada sprint.
- **Custos registrados:** o relatório documenta o preço do desenho — a camada extra de
  mapeamento (DTO ↔ command/view ↔ domínio) e a raiz de composição a manter à mão.

## v1.0 — projeto planejado (scaffold)

- Estrutura de pacotes do hexágono (domínio, aplicação, adapters), build e infraestrutura
  Docker; treze etapas descritas em caráter de planejamento.
- Decisões então previstas — Spring Data JPA, MapStruct, Lombok, ids `BIGINT` — todas
  revistas na v2.0.

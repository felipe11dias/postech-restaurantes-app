# Changelog — Relatório Técnico (Tech Challenge Fase 1)

Histórico de versões do relatório. Formato: versão · data · mudanças.

## v1.1
- **Segurança — autorização por posse (IDOR):** operações por id
  (`GET/PUT/PATCH/DELETE /users/{id}`) passam a exigir ser o dono do recurso ou `ROLE_ADMIN`
  (`@EnableMethodSecurity` + `@PreAuthorize` + novo bean `UserSecurity`). Documentado na
  Etapa 6 e refletido na tabela de endpoints (Etapa 8).
- **Segurança — autocadastro restrito:** o cadastro público rejeita `ROLE_ADMIN`
  (`ForbiddenOperationException` → 403), evitando escalonamento de privilégio. Documentado
  nas Etapas 6, 7 e 9.
- Nova migration `V3__seed_admin_user.sql` (usuário `admin.demo` / `ROLE_ADMIN`) para os
  cenários de escopo administrativo. Tabela de migrations e de usuários de demonstração
  atualizadas (Etapa 3).
- Tabela de erros (Etapa 9) acrescida das respostas `403` (`ForbiddenOperationException` e
  `AccessDeniedException`).
- Coleção Postman reescrita: fluxo com `{{adminToken}}`/`{{userId}}`/`{{token}}` e novos
  cenários de posse (403) e de admin (200/404). Testes unitários acrescidos do caso de
  bloqueio de `ROLE_ADMIN` no autocadastro (Etapas 12 e 13).
- Projeto migrado para o monorepo `postech/` (pasta `fase-01/`); repositório Git inicializado.

## v1.0
- Estrutura inicial do relatório com todas as seções exigidas no entregável.
- Arquitetura em camadas documentada (camadas + componentes transversais + diagramas).
- Modelagem completa do banco: tabelas `users`, `roles`, `user_roles`, `addresses`,
  com justificativa de normalização (1FN, 2FN, 3FN, BCNF) e diagrama ER.
- Planificação de objetos: entidades, enum `RoleName`, VOs de valor (`Email`, `ZipCode`),
  VOs de contrato versionados (`vo/v1/request`, `vo/v1/response`) e classes utilitárias.
- Decisões técnicas registradas (versionamento de API e de VOs, unificação em VOs,
  utilitários, ProblemDetail, BCrypt).
- Passo a passo de execução com Docker Compose e variáveis de ambiente.
- Stack incrementada: Flyway (schema versionado, Hibernate em `validate`), Spring HATEOAS
  e Spring Boot Actuator. Primeira migration `V1__create_initial_schema.sql` adicionada.
- MapStruct adotado para mapeamento VO ↔ entidade (substituindo a proposta de DozerMapper,
  incompatível com records). Processadores Lombok + MapStruct configurados no build.
- ArchUnit incluído com testes de regras da arquitetura em camadas (`ArchitectureTest`).
- Nome do banco padrão alterado para `restaurantes-app`.
- Nomenclatura dos objetos de transferência padronizada para "VO" em todo o relatório.
- Etapa 3: repositories `UserRepository` e `RoleRepository` implementados.
- Etapa 4: VOs de request/response (v1), validação com Bean Validation e mapeadores
  MapStruct (`UserMapper`, `AddressMapper`) implementados.
- Roteiro reordenado: "Login + Security + JWT" promovido para a Etapa 5.
- Etapa 5: camada de autenticação implementada — Spring Security stateless, BCrypt,
  `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`,
  `AuthenticationService` e `AuthController` (`POST /api/v1/auth/login`).
- Relatório reestruturado por etapas: o corpo passa a seguir as 12 etapas do Sumário de
  Progresso, com um Mapa dos Entregáveis Obrigatórios e referências cruzadas consistentes.
- Etapa 6: `UserService` com as regras de negócio (cadastro com hash BCrypt, unicidade de
  e-mail/login, troca de senha, atualização de dados, busca por nome) e exceções de domínio.
- Etapa 7: `UserController` versionado (`/api/v1/users`) com CRUD, endpoint exclusivo de
  senha e busca por nome, mais HATEOAS via `UserModelAssembler`.
- Etapa 8: `GlobalExceptionHandler` e `JwtAuthenticationEntryPoint` padronizando os erros
  como ProblemDetail (RFC 7807).
- Etapa 9: `OpenApiConfig` com Swagger e esquema de segurança Bearer JWT.
- Etapa 11: testes unitários `UserServiceTest` (JUnit 5 + Mockito) cobrindo as regras de
  negócio, somados ao `ArchitectureTest` (ArchUnit).
- Etapa 12: coleção Postman com todos os cenários e README do repositório.
- Etapa 13: migration de seed `V2__seed_demo_users.sql` (usuários de demonstração com hash
  BCrypt) e instruções de uso do Flyway no relatório e no README.
- Todas as 13 etapas concluídas.
- Etapa "Migrations e Seeds (Flyway)" realocada para a posição 3 (logo após a Modelagem);
  demais etapas renumeradas e referências cruzadas ajustadas. Restaurado o título da seção
  "Decisões Técnicas".
- Listagem e busca de usuários agora paginadas (Spring Data `Pageable` + `PagedModel`
  HATEOAS, com parâmetros `page`/`size`/`sort`).

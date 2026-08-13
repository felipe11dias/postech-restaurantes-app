# Changelog — Relatório Técnico (Tech Challenge Fase 1)

Histórico de versões do relatório. Formato: versão · data · mudanças.

## v2.0
- **Identificadores sequenciais → UUID:** todas as chaves primárias (e as estrangeiras que
  as referenciam) passam de `BIGINT GENERATED ALWAYS AS IDENTITY` para `UUID`, gerado pelo
  banco via `DEFAULT gen_random_uuid()`. Motivação: ids sequenciais expostos nas rotas
  permitiam enumeração de recursos. Nova migration `V6__convert_ids_to_uuid.sql`, que traduz
  os vínculos existentes antes de descartar as colunas antigas (seeds preservados).
  Documentado nas Etapas 2 e 3.
- **Spring Data JPA → JDBC:** o ORM foi removido. Cada repositório passa a ser uma interface
  com implementação em `JdbcTemplate` e SQL escrito à mão. Cascade, `orphanRemoval`, o N:M de
  papéis, o carregamento das associações (evitando N+1) e a auditoria tornam-se explícitos.
  `Address` e `PasswordResetToken` passam a referenciar o usuário por id. Etapa 4 reescrita.
- **Remoção do Lombok:** entidades com construtores, getters, setters e builder escritos à
  mão; `MailServiceImpl` com logger via `LoggerFactory`. O builder foi preservado por
  convenção para que o MapStruct continuasse gerando os mapeadores sem alteração.
  Documentado na Etapa 5.
- **Segurança — ordenação da listagem:** o nome da coluna do `ORDER BY` não é parametrizável
  em SQL, então passou a vir de uma lista fixa de propriedades permitidas, e nunca direto da
  requisição. Documentado na Etapa 4.
- **Segurança — senha fora da listagem:** a consulta paginada deixou de ler a coluna
  `password`. `findById`/`findByLogin` continuam lendo, pois alimentam a gravação e a
  autenticação. Documentado na Etapa 4.
- **Correção:** parâmetro de rota malformado (`{id}` que não é UUID) passa a retornar `400`
  em vez de `500`, via tratamento de `MethodArgumentTypeMismatchException`. Etapa 9.
- **Correção:** corpo de requisição ausente ou com JSON malformado
  (`HttpMessageNotReadableException`) passa a retornar `400` em vez de `500`, sem repassar
  a mensagem do parser ao cliente. Etapa 9.
- **Correção — observabilidade:** o handler genérico registrava a resposta mas nunca a
  exceção, de modo que um `500` não deixava rastro e a causa se perdia. Passa a logar a
  exceção completa em nível ERROR, mantendo a resposta genérica ao cliente. Etapa 9.
- Novo teste unitário `GlobalExceptionHandlerTest` (5 casos), cobrindo os dois tratamentos
  acima e a garantia de que nem a mensagem do parser nem a da exceção interna vazam na
  resposta. Suíte passa de 18 para 23 testes. Etapa 12.
- Auditoria deixa de ser automática (o `AuditingEntityListener` era um recurso do ORM) e
  passa a ser aplicada pelos repositórios; `SpringSecurityAuditorAware` vira `AuditorProvider`
  e o teste correspondente vira `AuditorProviderTest`. Etapas 2 e 12.
- Regra de ArchUnit `repositories_sao_interfaces` ajustada para admitir as implementações
  `*Jdbc`, preservando a intenção original. Etapas 4 e 12.
- Coleção Postman: quatro requests com ids numéricos fixos (cenários de 403 e 404) ajustados
  para UUID. Etapa 13.
- Registradas como **pendências** a ausência de teste automatizado sobre o SQL escrito à mão
  (com o detalhamento da verificação manual realizada em seu lugar) e os prints da coleção
  Postman desatualizados. Etapas 12 e 13.

## v1.2
- **Auditoria (JPA):** todas as entidades (`User`, `Role`, `Address`,
  `PasswordResetToken`) passam a estender uma classe base `Auditable` e são
  auditadas via Spring Data JPA Auditing (`@EnableJpaAuditing`, `@CreatedDate`,
  `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`), substituindo os antigos
  `@PrePersist`/`@PreUpdate` manuais. `created_by`/`last_updated_by` vêm de um
  `AuditorAware<String>` que lê o login autenticado via `SecurityContextHolder`,
  com fallback `"system"` — inclusive quando o contexto traz o token anônimo do
  Spring Security (endpoints públicos), evitando gravar `"anonymousUser"`.
  Documentado na Etapa 2 (Auditoria) e refletido no diagrama ER.
- Novo teste unitário `SpringSecurityAuditorAwareTest` cobrindo os três cenários
  de resolução do auditor (Etapa 12).
- Nova migration `V5__add_audit_columns.sql` (colunas de auditoria em todas as
  tabelas) e `V4__create_password_reset_tokens.sql` (que já existia mas nunca
  havia sido documentada) — tabela de migrations da Etapa 3 atualizada.
- Diagrama ER e tabela de tabelas do modelo (Etapa 2) passam a incluir
  `password_reset_tokens`.
- Novo `CHANGELOG.md` na raiz do módulo `camadas/restaurantes`, documentando o
  histórico das migrations Flyway (`V1`–`V5`).

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

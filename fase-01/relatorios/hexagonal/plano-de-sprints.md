# Plano de Sprints — Fase 1 em Arquitetura Hexagonal

**Projeto:** `fase-01/hexagonal/restaurantes/`
**Objetivo:** construir a variante em Ports & Adapters do sistema de gestão de restaurantes,
com paridade funcional com a variante em camadas.

> Este documento é o roteiro de execução: cada sprint tem escopo fechado, critérios de
> aceite verificáveis e uma ordem de dependência explícita. Serve tanto como plano (o que
> fazer a seguir) quanto como registro de revisão (o que já foi entregue e como conferir).

---

## Como as sprints foram cortadas

O corte segue as **fronteiras da arquitetura**, não os tipos técnicos de arquivo. Uma sprint
entrega um anel inteiro do hexágono — o domínio, depois os ports, depois cada adapter — em
vez de "todas as entidades", "todos os controllers".

A razão é a regra de dependência. Como as setas apontam para dentro, cada anel só depende
dos que já existem: o domínio compila sozinho; os ports compilam sobre o domínio; os
adapters compilam sobre os ports. **Toda sprint termina com o projeto compilando e os
testes passando** — não existe um estado intermediário quebrado esperando a sprint seguinte.

O efeito colateral útil é que a ordem das sprints *é* a demonstração da arquitetura: o
núcleo foi escrito e testado antes de existir qualquer banco, HTTP ou framework.

---

## Quadro geral

| Sprint | Entrega | Anel do hexágono | Depende de | Status |
|--------|---------|------------------|------------|--------|
| 0 | Build e infraestrutura | — | — | ✅ |
| 1 | Domínio puro | Núcleo | 0 | ✅ |
| 2 | Ports de entrada (casos de uso) | Aplicação | 1 | ✅ |
| 3 | Ports de saída | Aplicação | 1 | ✅ |
| 4 | Serviços de aplicação | Aplicação | 2, 3 | ✅ |
| 5 | Adapter de persistência + migrations | Adapter de saída | 3 | ✅ |
| 6 | Adapters de segurança e e-mail | Adapter de saída | 3 | ✅ |
| 7 | Adapter web REST | Adapter de entrada | 2 | ✅ |
| 8 | Testes e verificação de arquitetura | Transversal | 1–7 | ✅ |
| 9 | Relatório, README e Postman | — | 1–8 | ✅ |

**Legenda:** ✅ concluída · 🔄 em andamento · ⏳ pendente.

---

## Sprint 0 — Build e infraestrutura

**Escopo.** Alinhar o `pom.xml` da variante hexagonal à stack final da variante em camadas
(Java 21, JDBC, sem Lombok — e, aqui, também sem MapStruct); ajustar `application.yml`,
`.env.example` e `docker-compose.yml`.

**Decisão que define a sprint.** O scaffold original previa Spring Data JPA + MapStruct +
Lombok. A variante em camadas, porém, evoluiu para JDBC puro e Java sem Lombok. Manter
stacks diferentes tornaria a comparação entre as duas arquiteturas inútil: qualquer
diferença observada poderia ser atribuída à troca de tecnologia em vez do desenho.
**A stack passou a ser idêntica para que a única variável seja a arquitetura.**

**Critérios de aceite**
- [x] `mvn compile` passa.
- [x] Nenhuma dependência de JPA/Hibernate, Lombok ou MapStruct no `pom.xml`.
- [x] `docker compose up` sobe app + PostgreSQL com sufixo `-hex` (sem conflitar com camadas).

---

## Sprint 1 — Domínio puro

**Escopo.** `User`, `Address`, `Role`, `RoleName`, `PasswordResetToken`; Value Objects
`Email` e `ZipCode`; hierarquia de exceções de domínio sob `DomainException`.

**Regras que passaram a morar no domínio**
- Timestamps de criação e da última alteração nascem na entidade (`newUser` / `touch`), não
  no ORM nem no serviço.
- Sem setters públicos: o estado só muda por `updateProfile`, `changePassword`,
  `replaceAddresses` — métodos que expressam intenção e não deixam esquecer o `touch()`.
- `Email` normaliza para minúsculas; é isso que faz a regra de e-mail único funcionar.
- `ZipCode` guarda 8 dígitos crus e formata sob demanda.
- `PasswordResetToken` decide sozinho se ainda é utilizável (`isUsable`).

**Correções em relação ao scaffold anterior**
- `User.newUser` era método de instância — impossível de usar como fábrica. Agora é `static`.
- Ids passaram de `Long` para `UUID`, acompanhando a decisão da variante em camadas.
- `Address` deixou de ter referência de volta para `User`. Chave estrangeira é vocabulário
  de banco; quem conhece `addresses.user_id` é o adapter de persistência.

**Critérios de aceite**
- [x] Nenhum import de framework em `domain..` (verificado por ArchUnit na Sprint 8).
- [x] Testes de domínio rodam sem Spring.

---

## Sprint 2 — Ports de entrada (casos de uso)

**Escopo.** Um input port por caso de uso: `RegisterUserUseCase`, `UpdateUserUseCase`,
`ChangePasswordUseCase`, `DeleteUserUseCase`, `FindUserUseCase`, `AuthenticateUseCase`,
`RequestPasswordResetUseCase`, `ResetPasswordUseCase`. Mais os *commands* (entrada) e as
*views* (saída), e os tipos próprios de paginação `PageQuery`/`PageResult`.

**Por que ports separados.** Um `UserUseCase` com oito métodos obrigaria o controller que só
cadastra a depender de exclusão, busca e login. Interfaces pequenas são o que torna a
Segregação de Interfaces concreta em vez de decorativa.

**Por que `PageQuery` próprio.** Para o núcleo não importar `Pageable` do Spring Data.
Paginação é conceito de negócio; `Pageable` é a encarnação dele em um framework.

**Critérios de aceite**
- [x] Todos os ports são interfaces.
- [x] Nenhuma entidade de domínio atravessa a fronteira de saída (só views).
- [x] Nenhum tipo do Spring nas assinaturas.

---

## Sprint 3 — Ports de saída

**Escopo.** `LoadUserPort`, `SaveUserPort`, `DeleteUserPort`, `CheckUserExistsPort`,
`LoadRolePort`, `PasswordEncoderPort`, `TokenProviderPort`, `TokenVerifierPort`,
`ResetTokenGeneratorPort`, `LoadPasswordResetTokenPort`, `SavePasswordResetTokenPort`,
`SendPasswordResetMailPort`, `AuditorPort`, `TransactionPort`.

**Os dois ports que exigem justificativa**

- **`TransactionPort`.** Como os serviços de aplicação não têm anotações, `@Transactional`
  está fora de questão. Empurrar a transação para o adapter resolveria os casos de uma
  tabela só, mas não os que coordenam dois ports — redefinir senha grava o usuário *e*
  queima o token. Quem sabe que essas duas gravações formam uma unidade é o caso de uso;
  o *como* continua no adapter.
- **`AuditorPort` e `TokenVerifierPort`.** Nenhum caso de uso os chama: quem consome são os
  adapters (persistência e web). Declará-los como ports evita o que seria pior — um adapter
  dependendo diretamente do outro.

**Critérios de aceite**
- [x] Ports segregados por intenção, não um repositório monolítico.
- [x] Nenhum port menciona tecnologia concreta (JDBC, BCrypt, JWT, SMTP).

---

## Sprint 4 — Serviços de aplicação

**Escopo.** `RegisterUserService`, `UpdateUserService`, `ChangePasswordService`,
`DeleteUserService`, `FindUserService`, `AuthenticateUserService`, `PasswordResetService`.

**A decisão central.** Os serviços **não têm `@Service`**. São instanciados por `@Bean` na
`UseCaseConfiguration`. Custo: um arquivo de configuração a manter. Ganho: o núcleo compila
e roda sem Spring no classpath — o que transforma os testes de caso de uso em testes de
unidade de verdade, e é a propriedade que o ArchUnit protege.

**Critérios de aceite**
- [x] Nenhum serviço importa Spring.
- [x] Toda dependência é um port, injetado por construtor.
- [x] Nenhum SQL, HTTP ou detalhe de framework dentro dos serviços.

---

## Sprint 5 — Adapter de persistência + migrations

**Escopo.** `UserPersistenceAdapter` (implementa quatro ports de dados),
`RolePersistenceAdapter`, `PasswordResetTokenPersistenceAdapter`, `SpringTransactionAdapter`;
migrations Flyway `V1` (schema) e `V2` (seeds).

**Decisão sobre as migrations.** A variante em camadas chegou ao schema atual por seis
migrations sucessivas (ids `BIGINT` convertidos para `UUID`, auditoria adicionada depois).
Aqui o banco é novo e **nasce no formato final**: replicar a evolução histórica de outro
banco seria ficção — migration registra a história real de um schema.

**Critérios de aceite**
- [x] O que o ORM fazia implicitamente (cascade, orphanRemoval, `@ManyToMany`) está
      explícito em SQL.
- [x] Associações carregadas em duas consultas por página, não uma por usuário (sem N+1).
- [x] A listagem paginada não carrega o hash de senha.
- [x] `?sort=` só aceita colunas de uma allowlist (o nome da coluna entra no `ORDER BY` por
      concatenação e nunca pode vir da requisição).
- [x] Flyway aplica `V1` e `V2` em banco limpo.

---

## Sprint 6 — Adapters de segurança e e-mail

**Escopo.** `BCryptPasswordAdapter`, `JwtTokenAdapter` (emite e verifica),
`SecureRandomResetTokenAdapter`, `SecurityContextAuditorAdapter`,
`SmtpPasswordResetMailAdapter`.

**O que o hexágono mudou aqui.** Na variante em camadas, autenticar é delegado ao
`AuthenticationManager` do Spring Security — a regra vive dentro do framework. Aqui não há
`AuthenticationManager` nem `UserDetailsService`: o `AuthenticateUseCase` carrega o usuário
por um port, confere o hash por outro e pede o token a um terceiro. O Spring Security fica
com o que é de fato dele — decidir quais rotas exigem autenticação.

**Critérios de aceite**
- [x] A palavra "BCrypt" aparece só no adapter e no bean de configuração.
- [x] Toda menção a JWT começa e termina no `JwtTokenAdapter`.
- [x] O token de reset é persistido apenas como hash.
- [x] Falha de SMTP não propaga (ver nota de segurança na Sprint 8).

---

## Sprint 7 — Adapter web REST

**Escopo.** `UserRestController`, `AuthRestController`, DTOs v1 de request/response,
`RestExceptionHandler` (ProblemDetail RFC 7807), `UserModelAssembler` (HATEOAS),
`JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `ResourceOwnerChecker`,
`SecurityConfig`, `OpenApiConfig`, `UseCaseConfiguration`.

**Endpoints**

| Método | Rota | Caso de uso | Requisito da fase |
|--------|------|-------------|-------------------|
| `POST` | `/api/v1/users` | RegisterUser | cadastro (público) |
| `GET` | `/api/v1/users/{id}` | FindUser | consulta |
| `GET` | `/api/v1/users?name=` | FindUser | busca por nome (paginada) |
| `PUT` | `/api/v1/users/{id}` | UpdateUser | atualização (endpoint distinto) |
| `PATCH` | `/api/v1/users/{id}/password` | ChangePassword | troca de senha (exclusivo) |
| `DELETE` | `/api/v1/users/{id}` | DeleteUser | exclusão |
| `POST` | `/api/v1/auth/login` | Authenticate | validação de login |
| `POST` | `/api/v1/auth/forgot-password` | RequestPasswordReset | recuperação de senha |
| `POST` | `/api/v1/auth/reset-password` | ResetPassword | recuperação de senha |

**Critérios de aceite**
- [x] Os controllers injetam **interfaces de caso de uso**, nunca classes concretas.
- [x] Todo erro sai como ProblemDetail com `type` estável.
- [x] Nenhum DTO de saída expõe senha.

---

## Sprint 8 — Testes e verificação de arquitetura

**Escopo.** Testes de domínio sem Spring; testes de caso de uso com mocks dos ports;
`HexagonalArchitectureTest` com nove regras de ArchUnit.

**Resultado:** 80 testes, todos passando.

**As regras de ArchUnit não são decorativas.** Elas foram verificadas com um teste negativo
deliberado: uma classe temporária foi adicionada em `domain.model` chamando
`org.springframework.util.StringUtils`, e a regra `dominio_nao_depende_de_framework` falhou
apontando o método e a linha exatos. A classe foi removida em seguida. Sem essa conferência,
uma regra mal escrita passaria sempre e daria uma falsa sensação de proteção.

**Achado de segurança durante a verificação end-to-end.** Com o SMTP indisponível,
`POST /auth/forgot-password` respondia **500 para e-mail cadastrado** e **202 para e-mail
inexistente**. A diferença de status code permitia descobrir quais contas existem —
exatamente o vazamento que a resposta uniforme foi desenhada para evitar. Corrigido no
`SmtpPasswordResetMailAdapter` (a falha de envio é registrada e não repropagada) e coberto
por teste. **A variante em camadas tem o mesmo defeito e ainda não foi corrigida.**

**Segundo achado: `/actuator/health` respondia 503.** Com o SMTP indisponível, o indicador de
saúde do e-mail derrubava o status agregado para `DOWN`, mesmo com banco e aplicação
perfeitamente funcionais — o que faria qualquer orquestrador reiniciar o container por causa
de um canal auxiliar. Como a entrega de e-mail já é best-effort por decisão de projeto, o
indicador foi desligado (`management.health.mail.enabled: false`). Health voltou a `UP`.

**Critérios de aceite**
- [x] `mvn test` verde (80 testes).
- [x] Nenhum teste de caso de uso sobe contexto Spring.
- [x] Regras de ArchUnit comprovadamente sensíveis a violações.
- [x] `/actuator/health` responde `UP` na configuração padrão.

---

## Sprint 9 — Relatório, README e Postman

**Escopo.** Relatório técnico hexagonal reescrito, este plano de sprints, README do projeto,
README da fase atualizado e coleção Postman.

**Critérios de aceite**
- [x] O relatório descreve o que foi construído, não o que se pretendia construir.
- [x] Toda decisão relevante tem a justificativa e o custo registrados.
- [x] A coleção Postman cobre casos de sucesso e de erro.

---

## Verificação end-to-end (executada)

Com `docker compose up --build`, contra a aplicação real:

| Cenário | Esperado | Resultado |
|---------|----------|-----------|
| Cadastro válido | 201 + Location + e-mail normalizado | ✅ |
| Cadastro com e-mail duplicado (variando a caixa) | 409 | ✅ |
| Autocadastro com `ROLE_ADMIN` | 403 | ✅ |
| Campos inválidos | 400 com lista de campos | ✅ |
| Login válido / inválido | 200 / 401 | ✅ |
| Acesso sem token | 401 ProblemDetail | ✅ |
| Acesso ao recurso de outro usuário | 403 | ✅ |
| Admin acessando qualquer usuário | 200 | ✅ |
| Busca por nome com acento, paginada | 200 com metadados | ✅ |
| Atualização de dados | 200, senha intacta | ✅ |
| Troca de senha / senha atual errada | 204 / 400 | ✅ |
| Recuperação: forgot → reset → login | 202 → 204 → 200 | ✅ |
| Reuso de token de reset | 400 | ✅ |
| Exclusão / exclusão repetida | 204 / 404 | ✅ |
| Id não-UUID / JSON malformado | 400 / 400 | ✅ |
| Swagger UI e `/v3/api-docs` | acessíveis | ✅ |

---

## Se houver uma Sprint 10

Itens conscientemente fora do escopo desta fase, registrados para não se perderem:

- **Testes de integração do adapter de persistência** com Testcontainers. Hoje o SQL é
  verificado end-to-end via Docker, não por suíte automatizada.
- **Testes de contrato do adapter web** (`@WebMvcTest`) com os casos de uso mockados.
- **Revogação de token**: os papéis viajam no JWT, então uma mudança de papel só vale a
  partir do próximo login.
- **Rate limiting** em `/auth/login` e `/auth/forgot-password`.

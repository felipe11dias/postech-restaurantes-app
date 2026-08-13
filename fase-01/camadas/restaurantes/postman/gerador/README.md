# Gerador da coleção e dos prints

A coleção do Postman e os prints em `../prints/` **não são mantidos à mão**: os dois saem da
mesma definição de cenários (`cenarios.mjs`), executada contra a aplicação no ar. É isso que
garante que não divirjam — e que nenhum print mostre uma resposta que a API não tenha
realmente devolvido.

Quando um endpoint muda ou um caso novo aparece, edite `cenarios.mjs` e regenere. Não edite
`Restaurantes.postman_collection.json` diretamente: a próxima geração sobrescreve.

## Como regenerar

Requisitos: Node 18+ (usa o `fetch` nativo, sem dependências), Docker e a aplicação em pé
com o banco limpo.

```bash
# 1. Suba a aplicação (ver README do módulo). Os cenários assumem os seeds das migrations
#    e criam/removem os próprios usuários de teste.

# 2. Execute os cenários: escreve um HTML por caso e regenera a coleção
node gerar.mjs ./saida-html ../Restaurantes.postman_collection.json

# 3. Converta os HTML em PNG (1000x670)
docker run --rm -v "$PWD:/w" -w /w --entrypoint sh zenika/alpine-chrome -c \
  'for f in saida-html/*.html; do b=$(basename "$f" .html); \
   chromium-browser --headless --no-sandbox --disable-gpu --hide-scrollbars \
   --window-size=1000,670 --screenshot=/w/../prints/$b.png /w/$f; done'
```

O script falha (exit 1) se algum caso devolver um status diferente do declarado em
`esperado` — então ele também serve como um teste de fumaça da API inteira.

## Detalhes que importam

- **Ordem é significativa.** Os cenários compartilham estado (`vars`): o cadastro salva o
  `userId`, o login salva o token, a exclusão vem por último. Inserir um caso no meio pode
  quebrar os seguintes.
- **O token de redefinição de senha** nunca é retornado pela API. O script o lê do log da
  aplicação (`docker logs prints-app`), onde é registrado em `INFO`. Se o container tiver
  outro nome, ajuste `tokenDoLog()`.
- **A coleção usa variáveis** (`{{userId}}`, `{{outroId}}`) onde a execução usa UUID reais,
  via o campo `postman` do cenário. Ao adicionar um caso com id dinâmico, preencha esse
  campo — senão um UUID real vaza para a coleção e ela deixa de ser reexecutável.

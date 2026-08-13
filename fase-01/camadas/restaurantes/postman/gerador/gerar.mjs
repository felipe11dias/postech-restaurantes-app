// Executa todos os cenários contra a aplicação em execução, captura as
// respostas reais e gera (a) um HTML por caso, para virar print, e (b) a
// coleção Postman correspondente.
import { execSync } from 'node:child_process';
import { writeFileSync, mkdirSync, rmSync } from 'node:fs';
import { cenarios, vars } from './cenarios.mjs';

const BASE = 'http://localhost:8080';
const SAIDA_HTML = process.argv[2];
const SAIDA_COLECAO = process.argv[3];

mkdirSync(SAIDA_HTML, { recursive: true });

const CORES_METODO = {
  GET: '#2e7d32', POST: '#b26a00', PUT: '#1565c0', PATCH: '#6a1b9a', DELETE: '#c62828',
};

// O fetch do Node nem sempre preenche statusText; as frases abaixo mantêm o
// badge no formato "200 OK" usado nos prints.
const FRASES = {
  200: 'OK', 201: 'Created', 202: 'Accepted', 204: 'No Content',
  400: 'Bad Request', 401: 'Unauthorized', 403: 'Forbidden',
  404: 'Not Found', 409: 'Conflict', 500: 'Internal Server Error',
};

const slug = (s) => s
  .normalize('NFD').replace(/[̀-ͯ]/g, '')
  .toLowerCase()
  .replace(/\(\d{3}\)/g, '')
  .replace(/[^a-z0-9]+/g, '-')
  .replace(/^-+|-+$/g, '');

const esc = (s) => String(s)
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const resolver = (v) => (typeof v === 'function' ? v() : v);

function formatar(corpo) {
  if (corpo === null || corpo === undefined) return '(sem corpo)';
  if (typeof corpo === 'string') return corpo;
  return JSON.stringify(corpo, null, 2);
}

function html({ pasta, nome, metodo, url, corpoReq, status, statusTexto, corpoResp, ms, bytes, nota }) {
  const corMetodo = CORES_METODO[metodo] ?? '#444';
  const corStatus = status < 300 ? '#2e7d32' : '#c62828';
  return `<!doctype html>
<html lang="pt-BR"><head><meta charset="utf-8">
<style>
  * { box-sizing: border-box; }
  html, body { margin:0; padding:0; width:1000px; height:670px; overflow:hidden;
    background:#fff; color:#1a1a1a;
    font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
  .faixa { background:#f0f0f0; color:#666; font-size:11px; letter-spacing:.09em;
    text-transform:uppercase; padding:9px 18px; border-bottom:1px solid #e0e0e0; }
  h1 { font-size:19px; font-weight:600; margin:16px 18px 12px; }
  h1 .sub { color:#777; font-weight:400; }
  .linha { display:flex; align-items:center; gap:10px; margin:0 18px 14px; }
  .metodo { color:#fff; background:${corMetodo}; font-size:12px; font-weight:700;
    padding:6px 12px; border-radius:3px; letter-spacing:.04em; }
  .url { flex:1; background:#f4f4f4; border:1px solid #e6e6e6; border-radius:3px;
    padding:8px 12px; font-family:Consolas,"Courier New",monospace; font-size:12.5px;
    color:#333; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; }
  .colunas { display:flex; gap:16px; margin:0 18px; }
  .col { flex:1; min-width:0; }
  .rotulo { font-size:10.5px; letter-spacing:.09em; text-transform:uppercase;
    color:#777; margin-bottom:7px; }
  .meta { display:flex; align-items:center; gap:9px; margin-bottom:7px; }
  .status { color:#fff; background:${corStatus}; font-size:12px; font-weight:700;
    padding:5px 11px; border-radius:3px; }
  .tempo { color:#777; font-size:12px; }
  pre { margin:0; background:#16211a; color:#e2e8e3; border-radius:4px;
    padding:14px 16px; font-family:Consolas,"Courier New",monospace; font-size:12px;
    line-height:1.55; max-height:372px; overflow:hidden; white-space:pre-wrap;
    word-break:break-word; }
  pre.resposta { height:372px; }
  .nota { margin:12px 18px 0; font-size:11.5px; color:#666; font-style:italic; }
</style></head><body>
  <div class="faixa">${esc(pasta)}</div>
  <h1>${esc(nome)}</h1>
  <div class="linha">
    <span class="metodo">${esc(metodo)}</span>
    <span class="url">${esc(url)}</span>
  </div>
  <div class="colunas">
    <div class="col">
      <div class="rotulo">Request body</div>
      <pre>${esc(corpoReq)}</pre>
    </div>
    <div class="col">
      <div class="rotulo">Response</div>
      <div class="meta">
        <span class="status">${status} ${esc(statusTexto)}</span>
        <span class="tempo">${ms} ms · ${bytes} B</span>
      </div>
      <pre class="resposta">${esc(corpoResp)}</pre>
    </div>
  </div>
  ${nota ? `<div class="nota">${esc(nota)}</div>` : ''}
</body></html>`;
}

function tokenDoLog() {
  const logs = execSync('docker logs prints-app 2>&1', { maxBuffer: 64 * 1024 * 1024 }).toString();
  const todos = [...logs.matchAll(/token: ([A-Za-z0-9_-]+)\)/g)];
  if (!todos.length) throw new Error('token de redefinição não encontrado no log da aplicação');
  return todos[todos.length - 1][1];
}

const falhas = [];
let indice = 0;

for (const c of cenarios) {
  indice += 1;
  const caminho = resolver(c.caminho);
  const url = BASE + caminho;
  const corpo = resolver(c.corpo);

  const headers = {};
  if (corpo !== null && corpo !== undefined) headers['Content-Type'] = 'application/json';
  if (c.auth === 'admin') headers.Authorization = `Bearer ${vars.adminToken}`;
  if (c.auth === 'user') headers.Authorization = `Bearer ${vars.token}`;

  const inicio = Date.now();
  const resp = await fetch(url, {
    method: c.metodo,
    headers,
    body: corpo === null || corpo === undefined
      ? undefined
      : (typeof corpo === 'string' ? corpo : JSON.stringify(corpo)),
  });
  const ms = Date.now() - inicio;
  const texto = await resp.text();

  if (resp.status !== c.esperado) {
    falhas.push(`${indice}. ${c.nome}: esperado ${c.esperado}, veio ${resp.status} — ${texto.slice(0, 200)}`);
  }

  let json = null;
  try { json = texto ? JSON.parse(texto) : null; } catch { /* resposta não-JSON */ }
  if (c.captura && json !== null) c.captura(json, vars);
  else if (c.captura) c.captura({}, vars);
  if (c.capturaToken) vars.resetToken = tokenDoLog();

  const arquivo = `${String(indice).padStart(2, '0')}-${slug(c.nome)}-${resp.status}`;
  writeFileSync(`${SAIDA_HTML}/${arquivo}.html`, html({
    pasta: c.pasta,
    nome: c.nome,
    metodo: c.metodo,
    url,
    corpoReq: formatar(corpo),
    status: resp.status,
    statusTexto: resp.statusText || FRASES[resp.status] || '',
    corpoResp: texto ? (json ? JSON.stringify(json, null, 2) : texto) : '(sem corpo)',
    ms,
    bytes: Buffer.byteLength(texto, 'utf8'),
    nota: c.nota,
  }), 'utf8');

  console.log(`${String(indice).padStart(2, '0')} ${resp.status} ${c.metodo.padEnd(6)} ${c.nome}`);
}

// ----------------------------------------------------------- coleção Postman
const pastas = [];
for (const c of cenarios) {
  let pasta = pastas.find((p) => p.name === c.pasta);
  if (!pasta) { pasta = { name: c.pasta, item: [] }; pastas.push(pasta); }

  const caminho = c.postman ?? (typeof c.caminho === 'function' ? resolver(c.caminho) : c.caminho);
  const [semQuery, query] = caminho.split('?');
  const corpo = c.postmanCorpo ?? (typeof c.corpo === 'function' ? resolver(c.corpo) : c.corpo);

  const header = [];
  if (corpo !== null && corpo !== undefined) header.push({ key: 'Content-Type', value: 'application/json' });
  if (c.auth === 'admin') header.push({ key: 'Authorization', value: 'Bearer {{adminToken}}' });
  if (c.auth === 'user') header.push({ key: 'Authorization', value: 'Bearer {{token}}' });

  const url = {
    raw: `{{baseUrl}}${caminho}`,
    host: ['{{baseUrl}}'],
    path: semQuery.split('/').filter(Boolean),
  };
  if (query) {
    url.query = query.split('&').map((par) => {
      const [key, ...resto] = par.split('=');
      return { key, value: resto.join('=') };
    });
  }

  const item = { name: c.nome, request: { method: c.metodo, header, url } };
  if (corpo !== null && corpo !== undefined) {
    item.request.body = {
      mode: 'raw',
      raw: typeof corpo === 'string' ? corpo : JSON.stringify(corpo, null, 2),
    };
  }
  if (c.script) {
    item.event = [{ listen: 'test', script: { type: 'text/javascript', exec: c.script } }];
  }
  pasta.item.push(item);
}

const colecao = {
  info: {
    name: 'Restaurantes — Tech Challenge Fase 1',
    description: [
      'Coleção de testes da API de gestão de usuários — cobre todos os casos de cada endpoint.',
      '',
      'Execute de cima para baixo: os scripts de teste populam {{adminToken}}, {{userId}},',
      '{{token}} e {{outroId}} automaticamente.',
      '',
      'Identificadores são UUID. A API aplica autorização por posse: um usuário só acessa o',
      'próprio {id}; ROLE_ADMIN acessa qualquer um.',
      '',
      'Os casos de redefinição de senha dependem do token bruto enviado por e-mail, que a API',
      'nunca retorna — copie-o do log da aplicação para a variável {{resetToken}}.',
    ].join('\n'),
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  item: pastas,
  variable: [
    { key: 'baseUrl', value: 'http://localhost:8080' },
    { key: 'token', value: '' },
    { key: 'adminToken', value: '' },
    { key: 'userId', value: '' },
    { key: 'outroId', value: '' },
    { key: 'resetToken', value: '' },
  ],
};

writeFileSync(SAIDA_COLECAO, JSON.stringify(colecao, null, 2) + '\n', 'utf8');

console.log(`\n${cenarios.length} cenários executados.`);
if (falhas.length) {
  console.log(`\nDIVERGENCIAS DE STATUS (${falhas.length}):`);
  falhas.forEach((f) => console.log('  ' + f));
  process.exitCode = 1;
} else {
  console.log('Todos os status vieram como esperado.');
}

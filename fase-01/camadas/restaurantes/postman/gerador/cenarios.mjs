// Fonte única de verdade dos cenários: alimenta tanto a coleção Postman
// quanto os prints, para que os dois não possam divergir.
//
// Campos:
//   pasta      pasta na coleção Postman
//   nome       título do caso (aparece no print e na coleção)
//   metodo     verbo HTTP
//   caminho    caminho real usado na execução (resolvido a partir de vars)
//   postman    caminho com as variáveis {{...}} da coleção (default: caminho)
//   corpo      objeto JSON, string crua (para corpo malformado) ou null
//   auth       null (sem header) | 'admin' | 'user'
//   esperado   status HTTP esperado — a execução falha se divergir
//   captura    (json, vars) => void, guarda variáveis para os próximos casos
//   script     linhas do test script da coleção Postman

export const USUARIO = {
  nome: 'João Silva',
  email: 'joao.silva@email.com',
  login: 'joao.silva',
  senha: 'senhaSegura123',
};

export const OUTRO = {
  nome: 'Maria Souza',
  email: 'maria.souza@email.com',
  login: 'maria.souza',
  senha: 'senhaSegura123',
};

const UUID_INEXISTENTE = '00000000-0000-4000-8000-000000000000';

const endereco = {
  street: 'Rua das Flores',
  number: '100',
  complement: 'Apto 21',
  neighborhood: 'Centro',
  city: 'São Paulo',
  state: 'SP',
  zipCode: '01001-000',
};

const cadastro = (u, roles = ['ROLE_CUSTOMER']) => ({
  name: u.nome,
  email: u.email,
  login: u.login,
  password: u.senha,
  roles,
  addresses: [endereco],
});

const salvar = (variavel, campo) => [
  `if (pm.response.code === ${campo.codigo}) {`,
  '  const json = pm.response.json();',
  `  pm.collectionVariables.set('${variavel}', json.${campo.campo});`,
  `  console.log('${variavel} salvo.');`,
  '}',
];

export const cenarios = [
  // ---------------------------------------------------------------- Autenticação
  {
    pasta: 'Autenticação',
    nome: 'Login admin (seed) — salva adminToken',
    metodo: 'POST',
    caminho: '/api/v1/auth/login',
    corpo: { login: 'admin.demo', password: 'admin12345' },
    esperado: 200,
    captura: (json, vars) => { vars.adminToken = json.token; },
    script: salvar('adminToken', { codigo: 200, campo: 'token' }),
  },
  {
    pasta: 'Autenticação',
    nome: 'Login inválido — credenciais incorretas (401)',
    metodo: 'POST',
    caminho: '/api/v1/auth/login',
    corpo: { login: 'admin.demo', password: 'senha-errada' },
    esperado: 401,
  },
  {
    pasta: 'Autenticação',
    nome: 'Login inválido — campos obrigatórios ausentes (400)',
    metodo: 'POST',
    caminho: '/api/v1/auth/login',
    corpo: { login: '', password: '' },
    esperado: 400,
  },

  // ---------------------------------------------------------------- Cadastro
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Cadastro válido (201) — salva userId',
    metodo: 'POST',
    caminho: '/api/v1/users',
    corpo: cadastro(USUARIO),
    esperado: 201,
    captura: (json, vars) => { vars.userId = json.id; },
    script: salvar('userId', { codigo: 201, campo: 'id' }),
  },
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Login do usuário criado — salva token',
    metodo: 'POST',
    caminho: '/api/v1/auth/login',
    corpo: { login: USUARIO.login, password: USUARIO.senha },
    esperado: 200,
    captura: (json, vars) => { vars.token = json.token; },
    script: salvar('token', { codigo: 200, campo: 'token' }),
  },
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Cadastro de segundo usuário (201) — salva outroId',
    metodo: 'POST',
    caminho: '/api/v1/users',
    corpo: cadastro(OUTRO),
    esperado: 201,
    captura: (json, vars) => { vars.outroId = json.id; },
    script: salvar('outroId', { codigo: 201, campo: 'id' }),
  },
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Cadastro inválido — campos obrigatórios ausentes (400)',
    metodo: 'POST',
    caminho: '/api/v1/users',
    corpo: { name: '', email: 'nao-e-email', login: 'x', password: '123' },
    esperado: 400,
  },
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Cadastro inválido — corpo malformado (400)',
    metodo: 'POST',
    caminho: '/api/v1/users',
    corpo: '{ "name": "João", ',
    esperado: 400,
  },
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Cadastro inválido — e-mail duplicado (409)',
    metodo: 'POST',
    caminho: '/api/v1/users',
    corpo: cadastro({ ...USUARIO, login: 'outro.login' }),
    esperado: 409,
  },
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Cadastro inválido — login duplicado (409)',
    metodo: 'POST',
    caminho: '/api/v1/users',
    corpo: cadastro({ ...USUARIO, email: 'outro.email@email.com' }),
    esperado: 409,
  },
  {
    pasta: 'Usuários — Cadastro',
    nome: 'Cadastro com ROLE_ADMIN — proibido (403)',
    metodo: 'POST',
    caminho: '/api/v1/users',
    corpo: cadastro({ ...USUARIO, email: 'hacker@email.com', login: 'hacker' }, ['ROLE_ADMIN']),
    esperado: 403,
  },

  // ---------------------------------------------------------------- Consultas de coleção
  {
    pasta: 'Usuários — Consultas',
    nome: 'Listar paginado (200)',
    metodo: 'GET',
    caminho: '/api/v1/users?page=0&size=10&sort=name,asc',
    auth: 'user',
    esperado: 200,
  },
  {
    pasta: 'Usuários — Consultas',
    nome: 'Listar paginado — sem token (401)',
    metodo: 'GET',
    caminho: '/api/v1/users?page=0&size=10',
    esperado: 401,
  },
  {
    pasta: 'Usuários — Consultas',
    nome: 'Buscar por nome (200)',
    metodo: 'GET',
    caminho: '/api/v1/users?name=silva',
    auth: 'user',
    esperado: 200,
  },
  {
    pasta: 'Usuários — Consultas',
    nome: 'Buscar por nome — sem token (401)',
    metodo: 'GET',
    caminho: '/api/v1/users?name=silva',
    esperado: 401,
  },
  {
    pasta: 'Usuários — Consultas',
    nome: 'Ordenação por coluna não permitida — ignorada (200)',
    metodo: 'GET',
    caminho: '/api/v1/users?sort=password,asc',
    auth: 'user',
    esperado: 200,
    nota: 'A ordenação cai no padrão (name ASC): só propriedades da lista permitida entram no ORDER BY.',
  },

  // ---------------------------------------------------------------- Consulta por id
  {
    pasta: 'Usuários — Consulta por id',
    nome: 'Buscar por id — próprio usuário (200)',
    metodo: 'GET',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    auth: 'user',
    esperado: 200,
  },
  {
    pasta: 'Usuários — Consulta por id',
    nome: 'Buscar por id — via admin (200)',
    metodo: 'GET',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    auth: 'admin',
    esperado: 200,
  },
  {
    pasta: 'Usuários — Consulta por id',
    nome: 'Buscar por id de outro usuário — negado (403)',
    metodo: 'GET',
    caminho: () => `/api/v1/users/${vars.outroId}`,
    postman: '/api/v1/users/{{outroId}}',
    auth: 'user',
    esperado: 403,
  },
  {
    pasta: 'Usuários — Consulta por id',
    nome: 'Buscar por id inexistente — via admin (404)',
    metodo: 'GET',
    caminho: `/api/v1/users/${UUID_INEXISTENTE}`,
    auth: 'admin',
    esperado: 404,
  },
  {
    pasta: 'Usuários — Consulta por id',
    nome: 'Buscar por id em formato inválido (400)',
    metodo: 'GET',
    caminho: '/api/v1/users/nao-e-um-uuid',
    auth: 'admin',
    esperado: 400,
  },
  {
    pasta: 'Usuários — Consulta por id',
    nome: 'Buscar por id — sem token (401)',
    metodo: 'GET',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    esperado: 401,
  },

  // ---------------------------------------------------------------- Atualização
  {
    pasta: 'Usuários — Atualização',
    nome: 'Atualizar dados — próprio (200)',
    metodo: 'PUT',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    auth: 'user',
    corpo: {
      name: 'João Silva Atualizado',
      email: USUARIO.email,
      login: USUARIO.login,
      addresses: [{ ...endereco, street: 'Avenida Paulista', number: '1500' }],
    },
    esperado: 200,
  },
  {
    pasta: 'Usuários — Atualização',
    nome: 'Atualizar dados de outro usuário — negado (403)',
    metodo: 'PUT',
    caminho: () => `/api/v1/users/${vars.outroId}`,
    postman: '/api/v1/users/{{outroId}}',
    auth: 'user',
    corpo: { name: 'Tentativa', email: OUTRO.email, login: OUTRO.login, addresses: [endereco] },
    esperado: 403,
  },
  {
    pasta: 'Usuários — Atualização',
    nome: 'Atualizar dados inexistente — via admin (404)',
    metodo: 'PUT',
    caminho: `/api/v1/users/${UUID_INEXISTENTE}`,
    auth: 'admin',
    corpo: { name: 'Fantasma', email: 'fantasma@email.com', login: 'fantasma', addresses: [endereco] },
    esperado: 404,
  },
  {
    pasta: 'Usuários — Atualização',
    nome: 'Atualizar dados — campos inválidos (400)',
    metodo: 'PUT',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    auth: 'user',
    corpo: { name: '', email: 'nao-e-email', login: '', addresses: [] },
    esperado: 400,
  },
  {
    pasta: 'Usuários — Atualização',
    nome: 'Atualizar dados — sem token (401)',
    metodo: 'PUT',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    corpo: { name: 'Sem Token', email: USUARIO.email, login: USUARIO.login, addresses: [endereco] },
    esperado: 401,
  },

  // ---------------------------------------------------------------- Troca de senha
  {
    pasta: 'Usuários — Troca de senha',
    nome: 'Trocar senha — próprio, sucesso (204)',
    metodo: 'PATCH',
    caminho: () => `/api/v1/users/${vars.userId}/password`,
    postman: '/api/v1/users/{{userId}}/password',
    auth: 'user',
    corpo: { currentPassword: USUARIO.senha, newPassword: 'novaSenha456', confirmPassword: 'novaSenha456' },
    esperado: 204,
    captura: (_json, vars) => { vars.senhaAtual = 'novaSenha456'; },
  },
  {
    pasta: 'Usuários — Troca de senha',
    nome: 'Trocar senha — senha atual incorreta (400)',
    metodo: 'PATCH',
    caminho: () => `/api/v1/users/${vars.userId}/password`,
    postman: '/api/v1/users/{{userId}}/password',
    auth: 'user',
    corpo: { currentPassword: 'senha-errada', newPassword: 'outraSenha789', confirmPassword: 'outraSenha789' },
    esperado: 400,
  },
  {
    pasta: 'Usuários — Troca de senha',
    nome: 'Trocar senha — confirmação divergente (400)',
    metodo: 'PATCH',
    caminho: () => `/api/v1/users/${vars.userId}/password`,
    postman: '/api/v1/users/{{userId}}/password',
    auth: 'user',
    corpo: () => ({ currentPassword: vars.senhaAtual, newPassword: 'outraSenha789', confirmPassword: 'divergente999' }),
    esperado: 400,
  },
  {
    pasta: 'Usuários — Troca de senha',
    nome: 'Trocar senha de outro usuário — negado (403)',
    metodo: 'PATCH',
    caminho: () => `/api/v1/users/${vars.outroId}/password`,
    postman: '/api/v1/users/{{outroId}}/password',
    auth: 'user',
    corpo: { currentPassword: OUTRO.senha, newPassword: 'invadida123', confirmPassword: 'invadida123' },
    esperado: 403,
  },

  // ---------------------------------------------------------------- Recuperação de senha
  {
    pasta: 'Recuperação de senha',
    nome: 'Esqueci minha senha — e-mail existente (202)',
    metodo: 'POST',
    caminho: '/api/v1/auth/forgot-password',
    corpo: { email: USUARIO.email },
    esperado: 202,
    capturaToken: true,
  },
  {
    pasta: 'Recuperação de senha',
    nome: 'Esqueci minha senha — e-mail inexistente (202)',
    metodo: 'POST',
    caminho: '/api/v1/auth/forgot-password',
    corpo: { email: 'nao.existe@email.com' },
    esperado: 202,
    nota: 'A resposta é idêntica à do e-mail existente, para não revelar quais endereços estão cadastrados.',
  },
  {
    pasta: 'Recuperação de senha',
    nome: 'Esqueci minha senha — e-mail inválido (400)',
    metodo: 'POST',
    caminho: '/api/v1/auth/forgot-password',
    corpo: { email: 'nao-e-email' },
    esperado: 400,
  },
  {
    pasta: 'Recuperação de senha',
    nome: 'Redefinir senha — token inválido (400)',
    metodo: 'POST',
    caminho: '/api/v1/auth/reset-password',
    corpo: { token: 'token-que-nao-existe', newPassword: 'novaSenha456', confirmPassword: 'novaSenha456' },
    esperado: 400,
  },
  {
    pasta: 'Recuperação de senha',
    nome: 'Redefinir senha — senhas não coincidem (400)',
    metodo: 'POST',
    caminho: '/api/v1/auth/reset-password',
    corpo: () => ({ token: vars.resetToken, newPassword: 'novaSenha456', confirmPassword: 'divergente999' }),
    postmanCorpo: { token: '{{resetToken}}', newPassword: 'novaSenha456', confirmPassword: 'divergente999' },
    esperado: 400,
  },
  {
    pasta: 'Recuperação de senha',
    nome: 'Redefinir senha — sucesso (204)',
    metodo: 'POST',
    caminho: '/api/v1/auth/reset-password',
    corpo: () => ({ token: vars.resetToken, newPassword: 'senhaRedefinida789', confirmPassword: 'senhaRedefinida789' }),
    postmanCorpo: { token: '{{resetToken}}', newPassword: 'senhaRedefinida789', confirmPassword: 'senhaRedefinida789' },
    esperado: 204,
  },
  {
    pasta: 'Recuperação de senha',
    nome: 'Redefinir senha — token já utilizado (400)',
    metodo: 'POST',
    caminho: '/api/v1/auth/reset-password',
    corpo: () => ({ token: vars.resetToken, newPassword: 'maisUma123', confirmPassword: 'maisUma123' }),
    postmanCorpo: { token: '{{resetToken}}', newPassword: 'maisUma123', confirmPassword: 'maisUma123' },
    esperado: 400,
    nota: 'O token é de uso único: a segunda tentativa com o mesmo token é rejeitada.',
  },

  // ---------------------------------------------------------------- Exclusão
  {
    pasta: 'Usuários — Exclusão',
    nome: 'Deletar outro usuário — negado (403)',
    metodo: 'DELETE',
    caminho: () => `/api/v1/users/${vars.outroId}`,
    postman: '/api/v1/users/{{outroId}}',
    auth: 'user',
    esperado: 403,
  },
  {
    pasta: 'Usuários — Exclusão',
    nome: 'Deletar id inexistente — via admin (404)',
    metodo: 'DELETE',
    caminho: `/api/v1/users/${UUID_INEXISTENTE}`,
    auth: 'admin',
    esperado: 404,
  },
  {
    pasta: 'Usuários — Exclusão',
    nome: 'Deletar — sem token (401)',
    metodo: 'DELETE',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    esperado: 401,
  },
  {
    pasta: 'Usuários — Exclusão',
    nome: 'Deletar — próprio (204)',
    metodo: 'DELETE',
    caminho: () => `/api/v1/users/${vars.userId}`,
    postman: '/api/v1/users/{{userId}}',
    auth: 'user',
    esperado: 204,
    nota: 'Remove em cascata os endereços, vínculos de papel e tokens do usuário.',
  },

  // ---------------------------------------------------------------- Monitoramento
  {
    pasta: 'Monitoramento (Actuator)',
    nome: 'Health check (200)',
    metodo: 'GET',
    caminho: '/actuator/health',
    esperado: 200,
  },
  {
    pasta: 'Monitoramento (Actuator)',
    nome: 'Info (200)',
    metodo: 'GET',
    caminho: '/actuator/info',
    esperado: 200,
  },
  {
    pasta: 'Monitoramento (Actuator)',
    nome: 'Metrics — autenticado (200)',
    metodo: 'GET',
    caminho: '/actuator/metrics',
    auth: 'admin',
    esperado: 200,
  },
  {
    pasta: 'Monitoramento (Actuator)',
    nome: 'Metrics — sem token (401)',
    metodo: 'GET',
    caminho: '/actuator/metrics',
    esperado: 401,
    nota: 'Apenas /actuator/health e /actuator/info são públicos; os demais exigem token.',
  },

  // ---------------------------------------------------------------- Documentação
  {
    pasta: 'Documentação (OpenAPI)',
    nome: 'Especificação OpenAPI — pública (200)',
    metodo: 'GET',
    caminho: '/v3/api-docs',
    esperado: 200,
  },
];

// Variáveis compartilhadas entre os cenários durante a execução.
export const vars = {
  adminToken: '',
  token: '',
  userId: '',
  outroId: '',
  resetToken: '',
  senhaAtual: USUARIO.senha,
};

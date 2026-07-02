-- V3: Seed de usuário administrador de demonstração
--
-- Cria um usuário com ROLE_ADMIN, usado para exercitar os cenários de escopo
-- administrativo (acessar/alterar qualquer usuário, ver a lista completa).
-- O papel ROLE_ADMIN não pode ser obtido pelo autocadastro público — só por
-- seed (como aqui) ou por um fluxo administrativo autenticado.
--
-- Credenciais (apenas para demonstração — NÃO use em produção):
--   login: admin.demo  | senha: admin12345
--
-- Senha em hash BCrypt (mesmo algoritmo do BCryptPasswordEncoder).

INSERT INTO users (name, email, login, password, created_at, last_updated_at) VALUES
    ('Administrador Demonstração', 'admin@restaurante.com', 'admin.demo',
     '$2a$10$oqMT/2Q5GKhFarAGY1ye4OcCiJ.TPRBLY16maJ1KZbqtzIrX6QyxG', NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.login = 'admin.demo';

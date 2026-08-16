-- V2: Seed de usuários de demonstração
--
-- Cria um dono de restaurante, um cliente e um administrador, com papéis e
-- endereços, para que o login possa ser testado assim que a aplicação sobe.
--
-- Credenciais (apenas para demonstração — NÃO use em produção):
--   login: dono.restaurante | senha: dono12345
--   login: cliente.demo     | senha: cliente12345
--   login: admin.demo       | senha: admin12345
--
-- As senhas estão em hash BCrypt (o mesmo algoritmo do BCryptPasswordAdapter).
-- O papel ROLE_ADMIN não pode ser obtido pelo autocadastro público — só por seed
-- (como aqui) ou por um fluxo administrativo autenticado.

INSERT INTO users (name, email, login, password,
                   created_at, last_updated_at, created_by, last_updated_by) VALUES
    ('Dono Demonstração', 'dono@restaurante.com', 'dono.restaurante',
     '$2b$10$Q7SROUxFV0J7PyNzFvJruOnWhvxEAQLoHqGNzGx7yzlee.KM6AvKG',
     NOW(), NOW(), 'system', 'system'),
    ('Cliente Demonstração', 'cliente@email.com', 'cliente.demo',
     '$2b$10$jY0u2AlPFT9lETJgvfKdC.s/ntVtUj2gikZdtSna7/TlR.kXSJiKm',
     NOW(), NOW(), 'system', 'system'),
    ('Administrador Demonstração', 'admin@restaurante.com', 'admin.demo',
     '$2a$10$oqMT/2Q5GKhFarAGY1ye4OcCiJ.TPRBLY16maJ1KZbqtzIrX6QyxG',
     NOW(), NOW(), 'system', 'system');

-- Vínculos de papel (resolvendo os ids por login e nome do papel)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_OWNER'
 WHERE u.login = 'dono.restaurante';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_CUSTOMER'
 WHERE u.login = 'cliente.demo';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_ADMIN'
 WHERE u.login = 'admin.demo';

-- Endereços
INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code,
                       created_at, last_updated_at, created_by, last_updated_by)
SELECT u.id, 'Rua do Comércio', '500', NULL, 'Centro', 'São Paulo', 'SP', '01001000',
       NOW(), NOW(), 'system', 'system'
  FROM users u WHERE u.login = 'dono.restaurante';

INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code,
                       created_at, last_updated_at, created_by, last_updated_by)
SELECT u.id, 'Av. Paulista', '1500', 'Apto 42', 'Bela Vista', 'São Paulo', 'SP', '01310200',
       NOW(), NOW(), 'system', 'system'
  FROM users u WHERE u.login = 'cliente.demo';

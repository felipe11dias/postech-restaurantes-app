-- V2: Seed de usuários de demonstração
--
-- Cria um dono de restaurante e um cliente, com papéis e endereços, úteis para
-- testar o login imediatamente após subir a aplicação.
--
-- Credenciais (apenas para demonstração — NÃO use em produção):
--   login: dono.restaurante  | senha: dono12345
--   login: cliente.demo      | senha: cliente12345
--
-- As senhas estão em hash BCrypt (mesmo algoritmo do BCryptPasswordEncoder).

INSERT INTO users (name, email, login, password, created_at, last_updated_at) VALUES
    ('Dono Demonstração', 'dono@restaurante.com', 'dono.restaurante',
     '$2b$10$Q7SROUxFV0J7PyNzFvJruOnWhvxEAQLoHqGNzGx7yzlee.KM6AvKG', NOW(), NOW()),
    ('Cliente Demonstração', 'cliente@email.com', 'cliente.demo',
     '$2b$10$jY0u2AlPFT9lETJgvfKdC.s/ntVtUj2gikZdtSna7/TlR.kXSJiKm', NOW(), NOW());

-- Vínculos de papel (resolvendo os ids por login e nome do papel)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_OWNER'
WHERE u.login = 'dono.restaurante';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_CUSTOMER'
WHERE u.login = 'cliente.demo';

-- Endereços
INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code)
SELECT u.id, 'Rua do Comércio', '500', NULL, 'Centro', 'São Paulo', 'SP', '01001000'
FROM users u WHERE u.login = 'dono.restaurante';

INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code)
SELECT u.id, 'Av. Paulista', '1500', 'Apto 42', 'Bela Vista', 'São Paulo', 'SP', '01310200'
FROM users u WHERE u.login = 'cliente.demo';

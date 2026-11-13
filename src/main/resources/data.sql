-- =====================================================================
-- VOZ DO BAIRRO - dados de teste PostgreSQL (mesmo seed de banco/script-bd-base.sql).
-- Idempotente (INSERT ... SELECT ... WHERE NOT EXISTS): roda a cada subida sem duplicar e sem
-- gastar valores da identity. Senha de todos os usuarios: Senha@123.
-- =====================================================================

INSERT INTO "VB_BAIRRO" ("NOME", "SUBPREFEITURA", "META_APOIOS_PADRAO")
SELECT 'Vila Mariana', 'Vila Mariana', 50
WHERE NOT EXISTS (SELECT 1 FROM "VB_BAIRRO" WHERE "NOME" = 'Vila Mariana' AND "CIDADE" = 'São Paulo');

INSERT INTO "VB_BAIRRO" ("NOME", "SUBPREFEITURA", "META_APOIOS_PADRAO")
SELECT 'Capão Redondo', 'Campo Limpo', 30
WHERE NOT EXISTS (SELECT 1 FROM "VB_BAIRRO" WHERE "NOME" = 'Capão Redondo' AND "CIDADE" = 'São Paulo');

INSERT INTO "VB_BAIRRO" ("NOME", "SUBPREFEITURA", "META_APOIOS_PADRAO")
SELECT 'Itaquera', 'Itaquera', 40
WHERE NOT EXISTS (SELECT 1 FROM "VB_BAIRRO" WHERE "NOME" = 'Itaquera' AND "CIDADE" = 'São Paulo');

INSERT INTO "VB_USUARIO" ("NOME", "EMAIL", "SENHA_HASH", "PAPEL", "BAIRRO_ID")
SELECT 'Maria Cidadã', 'cidadao@vozdobairro.com', '$2a$10$Yo8Jrc6SrfqCEGZtaJFnReZPvqzVm5yvQpmwYdaTFgl78a5d17Ub6', 'CIDADAO',
       (SELECT "ID" FROM "VB_BAIRRO" WHERE "NOME" = 'Capão Redondo' AND "CIDADE" = 'São Paulo')
WHERE NOT EXISTS (SELECT 1 FROM "VB_USUARIO" WHERE "EMAIL" = 'cidadao@vozdobairro.com');

INSERT INTO "VB_USUARIO" ("NOME", "EMAIL", "SENHA_HASH", "PAPEL", "BAIRRO_ID")
SELECT 'João Vizinho', 'vizinho@vozdobairro.com', '$2a$10$Yo8Jrc6SrfqCEGZtaJFnReZPvqzVm5yvQpmwYdaTFgl78a5d17Ub6', 'CIDADAO',
       (SELECT "ID" FROM "VB_BAIRRO" WHERE "NOME" = 'Capão Redondo' AND "CIDADE" = 'São Paulo')
WHERE NOT EXISTS (SELECT 1 FROM "VB_USUARIO" WHERE "EMAIL" = 'vizinho@vozdobairro.com');

INSERT INTO "VB_USUARIO" ("NOME", "EMAIL", "SENHA_HASH", "PAPEL", "BAIRRO_ID")
SELECT 'Carla Gestora', 'gestor@vozdobairro.com', '$2a$10$Yo8Jrc6SrfqCEGZtaJFnReZPvqzVm5yvQpmwYdaTFgl78a5d17Ub6', 'GESTOR', NULL
WHERE NOT EXISTS (SELECT 1 FROM "VB_USUARIO" WHERE "EMAIL" = 'gestor@vozdobairro.com');

INSERT INTO "VB_PROPOSTA" ("TITULO", "DESCRICAO", "CATEGORIA", "META_APOIOS", "AUTOR_ID", "BAIRRO_ID")
SELECT 'Iluminação na Rua Agnaldo Macedo',
       'A rua fica totalmente escura à noite entre os números 100 e 400. Moradores voltando do trabalho e da escola se sentem inseguros.',
       'SEGURANCA', 30,
       (SELECT "ID" FROM "VB_USUARIO" WHERE "EMAIL" = 'cidadao@vozdobairro.com'),
       (SELECT "ID" FROM "VB_BAIRRO" WHERE "NOME" = 'Capão Redondo' AND "CIDADE" = 'São Paulo')
WHERE NOT EXISTS (SELECT 1 FROM "VB_PROPOSTA" WHERE "TITULO" = 'Iluminação na Rua Agnaldo Macedo');

INSERT INTO "VB_PROPOSTA" ("TITULO", "DESCRICAO", "CATEGORIA", "META_APOIOS", "AUTOR_ID", "BAIRRO_ID")
SELECT 'Faixa de pedestre em frente à EMEF',
       'Crianças atravessam a avenida sem faixa nem semáforo na saída da escola. Já houve atropelamentos.',
       'MOBILIDADE', 30,
       (SELECT "ID" FROM "VB_USUARIO" WHERE "EMAIL" = 'vizinho@vozdobairro.com'),
       (SELECT "ID" FROM "VB_BAIRRO" WHERE "NOME" = 'Capão Redondo' AND "CIDADE" = 'São Paulo')
WHERE NOT EXISTS (SELECT 1 FROM "VB_PROPOSTA" WHERE "TITULO" = 'Faixa de pedestre em frente à EMEF');

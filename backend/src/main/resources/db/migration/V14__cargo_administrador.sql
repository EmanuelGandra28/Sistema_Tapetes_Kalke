-- Cargo ADMINISTRADOR: mesmos poderes do DONO (o servidor libera todas as páginas e ações para ele).
-- Não precisa de linhas em permissao_pagina/permissao_acao.
INSERT INTO cargo(id,codigo,nome) VALUES ('00000000-0000-0000-0000-000000000006','ADMINISTRADOR','Administrador')
ON CONFLICT (codigo) DO NOTHING;

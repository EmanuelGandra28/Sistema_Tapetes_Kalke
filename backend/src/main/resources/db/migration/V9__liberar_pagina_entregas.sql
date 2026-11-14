-- A página Entregas ficou desligada para todos os cargos no banco publicado (o V5 a liberava para DONO, GERENTE e ENTREGADOR;
-- o único caminho que altera essa tabela é a tela de permissões). Sem ela nem o menu nem a API de listagem funcionam.
INSERT INTO permissao_pagina(cargo_codigo,pagina,permitido)
SELECT c.codigo,'entregas',true FROM cargo c WHERE c.codigo IN ('DONO','GERENTE','ENTREGADOR')
ON CONFLICT (cargo_codigo,pagina) DO UPDATE SET permitido = true;

-- Nova tela de despesas (salários, fretes, manutenção e outros), que alimenta as linhas "Salários" e "Outras despesas" do relatório.
INSERT INTO permissao_pagina(cargo_codigo,pagina,permitido)
SELECT c.codigo,'despesas',c.codigo IN ('DONO','GERENTE') FROM cargo c
ON CONFLICT (cargo_codigo,pagina) DO NOTHING;

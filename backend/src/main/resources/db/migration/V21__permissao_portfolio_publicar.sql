-- Botão "Atualizar site" (portfólio): por padrão só dono e gerente. O ADMINISTRADOR já tem todas as ações liberadas pelo servidor.
INSERT INTO permissao_acao(cargo_codigo,acao,permitido)
SELECT c.codigo,'portfolio_publicar',c.codigo IN ('DONO','GERENTE') FROM cargo c
ON CONFLICT (cargo_codigo,acao) DO NOTHING;

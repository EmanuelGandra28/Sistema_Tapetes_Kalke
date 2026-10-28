# Sistema Tapetes Kalke

Sistema de gestão da loja (vendas, estoque, compras, produção, entregas e relatórios). As telas são em React e a API em Java 21/Spring Boot.

## Onde cada parte fica no ar

| Parte | Serviço | Atualiza sozinho? |
|---|---|---|
| Telas (site) | GitHub Pages: `https://emanuelgandra28.github.io/Sistema_Tapetes_Kalke/` | Sim, a cada push na `main` |
| API Java | Render (plano grátis) | Sim, a cada push na `main` |
| Banco de dados | Supabase (PostgreSQL) | As tabelas são criadas e atualizadas pela própria API (Flyway) |

No plano grátis, o Render "adormece" a API depois de 15 minutos sem uso. O primeiro acesso depois disso leva cerca de 1 minuto; os seguintes são normais.

## Publicar pela primeira vez

Isso só precisa ser feito uma vez.

### 1. Banco no Supabase

1. Em [supabase.com/dashboard](https://supabase.com/dashboard), crie um projeto novo (por exemplo, `tapetes-kalke`). Anote a senha do banco escolhida na criação.
2. Abra **Connect** (botão no topo do projeto) e escolha **Session pooler**. Use esses dados no Render:
   - `DB_URL`: `jdbc:postgresql://<host do pooler>:5432/postgres?sslmode=require` (por exemplo, `aws-0-sa-east-1.pooler.supabase.com`).
   - `DB_USERNAME`: o usuário mostrado, no formato `postgres.<id-do-projeto>`.
   - `DB_PASSWORD`: a senha do banco.

   Use o **Session pooler**, não a "Direct connection": o Render não acessa a conexão direta.

Não é preciso rodar nenhum SQL no Supabase: a API cria todas as tabelas ao iniciar. A migration `V8` também liga o RLS em todas as tabelas, então elas não ficam expostas pela API REST automática do Supabase.

### 2. API no Render

1. Em [render.com](https://render.com), entre com a conta do GitHub.
2. Clique em **New > Blueprint** e escolha este repositório. O Render lê o `render.yaml` e cria o serviço `tapetes-kalke-api`.
3. Preencha as variáveis pedidas:
   - `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`: os dados do Supabase.
   - `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD`: o e-mail e a senha do primeiro usuário DONO. Use uma senha nova, que você não use em outros lugares.

   O `JWT_SECRET` é gerado automaticamente pelo Render.
4. Aguarde o primeiro deploy (alguns minutos). O endereço fica parecido com `https://tapetes-kalke-api.onrender.com`; abra `.../actuator/health` e confira se aparece `{"status":"UP"}`.

O `backend/Dockerfile` é só a receita que o Render usa para montar a API. Você não precisa instalar nem usar Docker.

### 3. Telas no GitHub Pages

1. No GitHub, abra **Settings > Secrets and variables > Actions > Variables** e crie a variável `API_URL` com o endereço do Render terminado em `/api/v1` (por exemplo, `https://tapetes-kalke-api.onrender.com/api/v1`).
2. Em **Settings > Pages**, escolha **GitHub Actions** como fonte.
3. Em **Actions > Publicar telas no GitHub Pages**, clique em **Run workflow** (ou faça um push que altere algo em `frontend/`).

### 4. Primeiro acesso

1. Entre com o e-mail e a senha do `BOOTSTRAP_ADMIN_*`.
2. Em **Configurações**, troque a senha e cadastre os demais usuários.
3. No Render, apague a variável `BOOTSTRAP_ADMIN_PASSWORD`: ela só é usada para criar o primeiro DONO.

## Rodar no computador (opcional)

Só é necessário para desenvolver. Para usar o sistema, basta o site publicado.

1. Copie `.env.example` para `.env` e preencha os segredos. `JWT_SECRET` é obrigatório: a API não inicia sem um segredo aleatório de ao menos 32 bytes.
2. Tenha um PostgreSQL 16 rodando (instalado no Windows) com o banco `tapetes_kalke` criado.
3. Em `backend`, execute `mvn spring-boot:run`.
4. Em `frontend`, execute `npm install` e `npm run dev`, e abra `http://localhost:5173`.

A documentação da API fica em `/swagger-ui/index.html` quando `API_DOCS_ENABLED=true`.

## Testes automáticos

A cada push, o GitHub roda os testes do backend e o lint e build das telas (**Actions > Testes**). Um X vermelho no commit indica que algo quebrou.

## Decisões de integridade

- Valores monetários e quantidades usam `BigDecimal`.
- Venda, compra, produção, cancelamentos e ajustes são transacionais.
- Produtos são bloqueados pessimisticamente durante alterações de saldo; o banco também impede saldos negativos.
- Estoque comercial e de produção são independentes e todo movimento gera histórico.
- Cancelamentos preservam histórico e emitem movimentos de estorno.
- A API é a fonte de verdade para preço, saldo, totais e permissões.

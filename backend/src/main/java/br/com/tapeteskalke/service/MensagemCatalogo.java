package br.com.tapeteskalke.service;

import java.util.*;

// Gatilhos de mensagem que o sistema conhece: nome, tipo, campos aceitos ({nome}, {loja}...), campos obrigatórios e texto padrão.
// A loja só altera o TEXTO (na aba Mensagens); o que cada gatilho aceita fica aqui, no código.
public final class MensagemCatalogo {
    private MensagemCatalogo() {}

    /** AVISO_PEDIDO: vai a quem fez a compra · CAMPANHA: só com consentimento, leva a frase de saída · INTERNA: para a equipe · ATENDIMENTO: enviada à mão por um funcionário. */
    public enum Tipo { AVISO_PEDIDO, CAMPANHA, INTERNA, ATENDIMENTO }

    public record Variavel(String nome, String descricao, String exemplo) {}

    public record Definicao(String chave, String nome, String descricao, Tipo tipo, List<Variavel> variaveis, List<String> obrigatorias, String textoPadrao) {
        public boolean exigeSaida() { return tipo == Tipo.CAMPANHA; }

        public Optional<Variavel> variavel(String nome) { return variaveis.stream().filter(v -> v.nome().equals(nome)).findFirst(); }
    }

    /** Frase de saída (LGPD): anexada pelo sistema a toda campanha; ninguém consegue apagá-la. */
    public static final String RODAPE_SAIDA = "Responda SAIR para não receber mais mensagens.";
    public static final int TAMANHO_MAXIMO = 700;

    private static final Variavel NOME = new Variavel("nome", "Primeiro nome do cliente", "Maria");
    private static final Variavel LOJA = new Variavel("loja", "Nome da loja", "Tapetes Kalke");
    private static final Variavel DIAS = new Variavel("dias_sem_comprar", "Dias desde a última compra", "75");
    private static final Variavel ULTIMO = new Variavel("ultimo_produto", "Último tapete que o cliente comprou", "Tapete Floral 2x3");
    private static final Variavel PRODUTO = new Variavel("produto", "Nome do tapete", "Tapete Floral 2x3");

    private static final Map<String, Definicao> TODAS = new LinkedHashMap<>();

    private static void add(String chave, String nome, String descricao, Tipo tipo, List<Variavel> variaveis, List<String> obrigatorias, String texto) {
        TODAS.put(chave, new Definicao(chave, nome, descricao, tipo, variaveis, obrigatorias, texto));
    }

    static {
        add("REATIVACAO_1", "Clientes sem compras nos últimos 2 meses — 1ª mensagem", "Primeiro contato com o cliente que parou de comprar.", Tipo.CAMPANHA,
                List.of(NOME, LOJA, DIAS, ULTIMO), List.of(),
                "Olá, {nome}! Aqui é da {loja}. Faz {dias_sem_comprar} dias que não nos vemos e queremos saber como estão os seus tapetes. Posso te ajudar com algo?");
        add("REATIVACAO_2", "Clientes sem compras nos últimos 2 meses — 2ª mensagem", "Segundo contato, para quem não respondeu o primeiro.", Tipo.CAMPANHA,
                List.of(NOME, LOJA, DIAS, ULTIMO), List.of(),
                "{nome}, tudo bem? Passando para contar que a {loja} tem novidades em tapetes. Quer ver as opções? É só responder esta mensagem.");
        add("REATIVACAO_3", "Clientes sem compras nos últimos 2 meses — 3ª mensagem", "Último contato da campanha de reativação.", Tipo.CAMPANHA,
                List.of(NOME, LOJA, DIAS, ULTIMO), List.of(),
                "{nome}, esta é a nossa última mensagem por aqui. Se precisar de um tapete novo, é só nos chamar. Um abraço da {loja}!");
        add("ENVIO_ENCOMENDA", "Envio de encomendas", "Avisa o cliente que a encomenda foi enviada, com o código de rastreio.", Tipo.AVISO_PEDIDO,
                List.of(NOME, LOJA, PRODUTO, new Variavel("codigo_rastreio", "Código de rastreio dos Correios", "AB123456789BR"),
                        new Variavel("link_rastreio", "Endereço para acompanhar a encomenda", "https://rastreio.exemplo.com.br/AB123456789BR")),
                List.of("codigo_rastreio"),
                "Olá, {nome}! A sua encomenda ({produto}) já foi enviada. Código de rastreio: {codigo_rastreio}. Acompanhe aqui: {link_rastreio}. Qualquer dúvida, estamos à disposição. {loja}");
        add("SAIU_PARA_ENTREGA", "Saiu para entrega", "Avisa o cliente no dia da entrega.", Tipo.AVISO_PEDIDO,
                List.of(NOME, LOJA, new Variavel("data_entrega", "Data da entrega", "25/11/2026"), new Variavel("horario", "Horário combinado", "14:00")),
                List.of("data_entrega"),
                "Olá, {nome}! O seu pedido sai para entrega em {data_entrega}, por volta das {horario}. Por favor, deixe alguém para receber. {loja}");
        add("COMPROVANTE_VENDA", "Comprovante de venda", "Agradecimento enviado depois da compra, com o resumo da venda.", Tipo.AVISO_PEDIDO,
                List.of(NOME, LOJA, new Variavel("numero_venda", "Número da venda", "1234"), new Variavel("total", "Valor total da venda", "R$ 450,00")),
                List.of("numero_venda"),
                "Olá, {nome}! Obrigado pela compra na {loja}. Venda nº {numero_venda}, no total de {total}. Volte sempre!");
        add("POS_VENDA", "Pós-venda", "Pergunta ao cliente, dias depois da entrega, se gostou do tapete.", Tipo.CAMPANHA,
                List.of(NOME, LOJA, PRODUTO), List.of(),
                "Olá, {nome}! Como está o seu {produto}? Gostou? A sua opinião é muito importante para a {loja}.");
        add("ANIVERSARIO", "Aniversário do cliente", "Parabéns no dia do aniversário.", Tipo.CAMPANHA,
                List.of(NOME, LOJA), List.of(),
                "Parabéns, {nome}! A equipe da {loja} deseja um dia muito especial para você. Quando quiser renovar a casa com um tapete novo, estamos à disposição.");
        add("BOTAO_WHATSAPP_CLIENTE", "Botão \"Chamar no WhatsApp\" (cliente sem comprar)", "Texto que já vem escrito quando um funcionário clica no botão do cliente.", Tipo.ATENDIMENTO,
                List.of(NOME, LOJA), List.of(),
                "Olá, {nome}! Aqui é da {loja}. Faz um tempinho que não nos vemos. Posso te ajudar com algum tapete?");
        add("ALERTA_PRODUCAO_DONA", "Alerta de produção para a dona", "Enviada à dona quando uma ordem de produção automática é criada.", Tipo.INTERNA,
                List.of(LOJA, new Variavel("lista_tapetes", "Lista dos tapetes com estoque baixo ou esgotado (montada pelo sistema)", "1) Tapete Floral 2x3 — saldo 2, mínimo 5"),
                        new Variavel("link", "Endereço da ordem de produção no sistema", "https://sistema.exemplo.com.br/producao")),
                List.of("lista_tapetes"),
                "Atenção, {loja}! Estes tapetes precisam ser produzidos:\n{lista_tapetes}\nAbra a ordem no sistema: {link}");
    }

    public static Collection<Definicao> todas() { return Collections.unmodifiableCollection(TODAS.values()); }

    public static Optional<Definicao> buscar(String chave) { return Optional.ofNullable(TODAS.get(chave)); }
}

package br.com.tapeteskalke.controller;

import static br.com.tapeteskalke.service.ExcelService.Tipo.*;

import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.VendaRepository;
import br.com.tapeteskalke.service.*;
import br.com.tapeteskalke.service.ExcelService.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

// Exportações para Excel. Cada uma exige a mesma permissão da tela/listagem correspondente.
@RestController
@RequestMapping("/api/v1")
public class ExportacaoController {
    private static final Map<String, String> FINANCEIRO = new LinkedHashMap<>();
    static {
        FINANCEIRO.put("receita", "Receita");
        FINANCEIRO.put("custoMercadoriaVendida", "Custo da mercadoria vendida");
        FINANCEIRO.put("lucroBruto", "Lucro bruto");
        FINANCEIRO.put("salarios", "Salários");
        FINANCEIRO.put("outrasDespesas", "Outras despesas");
        FINANCEIRO.put("lucroLiquido", "Lucro líquido");
        FINANCEIRO.put("compras", "Compras (caixa)");
        FINANCEIRO.put("fluxoCaixa", "Fluxo de caixa");
    }

    private final ExcelService excel;
    private final PermissionService permissions;
    private final VendaRepository vendas;
    private final ClienteService clientes;
    private final ProdutoService produtos;
    private final MovimentoService movimentos;
    private final SugestaoCompraService sugestao;
    private final RelatorioService financeiro;
    private final RelatorioAnaliticoService analitico;
    private final Clock clock;

    public ExportacaoController(ExcelService excel, PermissionService permissions, VendaRepository vendas, ClienteService clientes, ProdutoService produtos,
                                MovimentoService movimentos, SugestaoCompraService sugestao, RelatorioService financeiro, RelatorioAnaliticoService analitico, Clock clock) {
        this.excel = excel;
        this.permissions = permissions;
        this.vendas = vendas;
        this.clientes = clientes;
        this.produtos = produtos;
        this.movimentos = movimentos;
        this.sugestao = sugestao;
        this.financeiro = financeiro;
        this.analitico = analitico;
        this.clock = clock;
    }

    private LocalDate[] periodo(LocalDate inicio, LocalDate fim) {
        var hoje = LocalDate.now(clock);
        var de = inicio == null ? hoje.withDayOfMonth(1) : inicio;
        var ate = fim == null ? hoje : fim;
        if (de.isAfter(ate)) throw new BusinessException("PERIODO_INVALIDO", "A data inicial deve ser anterior ou igual à data final", HttpStatus.BAD_REQUEST);
        return new LocalDate[]{de, ate};
    }

    private static List<Object> linha(Object... valores) { return Arrays.asList(valores); }

    // Título da marca, período e data de geração no topo da planilha; totais opcionais no rodapé.
    private Aba com(String titulo, LocalDate[] p, boolean totais, Aba a) {
        var dia = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var agora = LocalDateTime.now(clock).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return new Aba(a.nome(), a.colunas(), a.linhas(), "Tapetes Kalke — " + titulo, "Período: " + p[0].format(dia) + " a " + p[1].format(dia) + " · Gerado em " + agora, totais);
    }

    @GetMapping("/vendas/exportar")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> vendas(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        permissions.page("vendas");
        var p = periodo(inicio, fim);
        var lista = vendas.doPeriodo(p[0], p[1]);
        var cabecalho = new Aba("Vendas", List.of(new Coluna("Número", NUMERO), new Coluna("Data", DATA), new Coluna("Cliente", TEXTO), new Coluna("Vendedor", TEXTO),
                new Coluna("Pagamento", TEXTO), new Coluna("Subtotal", MOEDA), new Coluna("Desconto", MOEDA), new Coluna("Total", MOEDA), new Coluna("Situação", TEXTO)),
                lista.stream().map(v -> linha(v.getNumeroVenda(), v.getDataVenda(), v.getCliente().getNome(), v.getVendedor() == null ? null : v.getVendedor().getNome(),
                        v.getFormaPagamento(), v.getSubtotal(), v.getDesconto(), v.getTotalVenda(), v.getStatus())).toList());
        var itens = new Aba("Itens", List.of(new Coluna("Venda", NUMERO), new Coluna("Data", DATA), new Coluna("Produto", TEXTO), new Coluna("Quantidade", NUMERO),
                new Coluna("Preço unitário", MOEDA), new Coluna("Subtotal", MOEDA), new Coluna("Situação", TEXTO)),
                lista.stream().flatMap(v -> v.getItens().stream().map(i -> linha(v.getNumeroVenda(), v.getDataVenda(), i.getProdutoNomeSnapshot(), i.getQuantidade(),
                        i.getPrecoUnitario(), i.getSubtotal(), v.getStatus()))).toList());
        return excel.exportar("vendas", cabecalho, itens);
    }

    @GetMapping("/clientes/exportar")
    public ResponseEntity<byte[]> clientes() {
        permissions.page("clientes");
        return excel.exportar("clientes", new Aba("Clientes", List.of(new Coluna("Nome", TEXTO), new Coluna("Telefone", TEXTO), new Coluna("E-mail", TEXTO),
                new Coluna("Endereço", TEXTO), new Coluna("Bairro", TEXTO), new Coluna("Cidade", TEXTO), new Coluna("CEP", TEXTO), new Coluna("Situação", TEXTO),
                new Coluna("Classificação", TEXTO), new Coluna("Última compra", DATA), new Coluna("Total comprado", MOEDA)),
                clientes.todos().stream().map(c -> linha(c.nome(), c.telefone(), c.email(), c.endereco(), c.bairro(), c.cidade(), c.cep(), c.statusCadastral(),
                        c.classificacao(), c.ultimaCompra(), c.totalCompras())).toList()));
    }

    @GetMapping("/estoque/exportar")
    public ResponseEntity<byte[]> estoque() {
        permissions.page("produtos");
        return excel.exportar("estoque", new Aba("Estoque", List.of(new Coluna("Produto", TEXTO), new Coluna("Tipo", TEXTO), new Coluna("Unidade", TEXTO),
                new Coluna("Estoque comercial", NUMERO), new Coluna("Mínimo comercial", NUMERO), new Coluna("Estoque produção", NUMERO), new Coluna("Mínimo produção", NUMERO),
                new Coluna("Preço de custo", MOEDA), new Coluna("Preço de venda", MOEDA), new Coluna("Situação", TEXTO), new Coluna("Ativo", TEXTO)),
                produtos.todos().stream().map(p -> linha(p.nome(), p.tipo(), p.unidadeMedida(), p.estoqueComercial(), p.estoqueMinimoComercial(), p.estoqueProducao(),
                        p.estoqueMinimoProducao(), p.precoCusto(), p.precoVenda(), p.statusComercial(), p.ativo() ? "Sim" : "Não")).toList()));
    }

    @GetMapping("/estoque/movimentos/exportar")
    public ResponseEntity<byte[]> movimentos(@RequestParam(required = false) String produtoId, @RequestParam(required = false) String saldo,
                                             @RequestParam(required = false) String tipo, @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        permissions.page("produtos");
        var lista = movimentos.todos(new MovimentoService.Filtro(produtoId, saldo, tipo, inicio, fim));
        return excel.exportar("movimentacoes", new Aba("Movimentações", List.of(new Coluna("Data", DATAHORA), new Coluna("Produto", TEXTO), new Coluna("Saldo", TEXTO),
                new Coluna("Tipo", TEXTO), new Coluna("Quantidade", NUMERO), new Coluna("Antes", NUMERO), new Coluna("Depois", NUMERO), new Coluna("Usuário", TEXTO), new Coluna("Observação", TEXTO)),
                lista.stream().map(m -> linha(m.data(), m.produto(), m.saldo(), m.tipo(), m.quantidade(), m.anterior(), m.posterior(), m.usuario(), m.observacao())).toList()));
    }

    @GetMapping("/estoque/sugestao-compra/exportar")
    public ResponseEntity<byte[]> sugestaoCompra() {
        permissions.page("produtos");
        return excel.exportar("sugestao-compra", new Aba("Sugestão de compra", List.of(new Coluna("Matéria-prima", TEXTO), new Coluna("Unidade", TEXTO),
                new Coluna("Em estoque", NUMERO), new Coluna("Ordens abertas", NUMERO), new Coluna("Mínimo", NUMERO), new Coluna("Comprar", NUMERO),
                new Coluna("Fornecedor", TEXTO), new Coluna("Custo unitário", MOEDA), new Coluna("Custo estimado", MOEDA)),
                sugestao.calcular().stream().map(s -> linha(s.nome(), s.unidade(), s.saldo(), s.comprometido(), s.minimo(), s.falta(), s.fornecedor(), s.custoUnitario(), s.custoEstimado())).toList()));
    }

    @GetMapping("/relatorios/financeiro/exportar")
    public ResponseEntity<byte[]> relFinanceiro(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        permissions.action("relatorios_visualizar");
        var p = periodo(inicio, fim);
        var dados = financeiro.financial(p[0], p[1]);
        var linhas = new ArrayList<List<Object>>();
        FINANCEIRO.forEach((chave, rotulo) -> linhas.add(linha(rotulo, dados.get(chave))));
        return excel.exportar("relatorio-financeiro", com("Resultado financeiro", p, false, new Aba("Financeiro", List.of(new Coluna("Indicador", TEXTO), new Coluna("Valor", MOEDA)), linhas)));
    }

    @GetMapping("/relatorios/curva-abc/exportar")
    public ResponseEntity<byte[]> relAbc(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        permissions.action("relatorios_visualizar");
        var p = periodo(inicio, fim);
        return excel.exportar("relatorio-curva-abc", com("Curva ABC — produtos que mais dão dinheiro", p, true, new Aba("Curva ABC", List.of(new Coluna("Classe", TEXTO), new Coluna("Produto", TEXTO), new Coluna("Quantidade", NUMERO),
                new Coluna("Receita", MOEDA), new Coluna("% da receita", NUMERO), new Coluna("% acumulado", NUMERO)),
                analitico.curvaAbc(p[0], p[1]).stream().map(a -> linha(a.classe(), a.produto(), a.quantidade(), a.receita(), a.percentual(), a.acumulado())).toList())));
    }

    @GetMapping("/relatorios/margem-produtos/exportar")
    public ResponseEntity<byte[]> relMargem(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        permissions.action("relatorios_visualizar");
        var p = periodo(inicio, fim);
        return excel.exportar("relatorio-margem", com("Margem por produto — quanto sobra em cada tapete", p, true, new Aba("Margem", List.of(new Coluna("Produto", TEXTO), new Coluna("Quantidade", NUMERO), new Coluna("Receita", MOEDA),
                new Coluna("Custo", MOEDA), new Coluna("Margem", MOEDA), new Coluna("Margem %", NUMERO), new Coluna("Custo estimado", TEXTO)),
                analitico.margem(p[0], p[1]).stream().map(m -> linha(m.produto(), m.quantidade(), m.receita(), m.custo(), m.margem(), m.margemPercentual(), m.custoEstimado() ? "Sim" : "Não")).toList())));
    }

    @GetMapping("/relatorios/vendas-por-vendedor/exportar")
    public ResponseEntity<byte[]> relVendedores(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        permissions.action("relatorios_visualizar");
        var p = periodo(inicio, fim);
        return excel.exportar("relatorio-vendedores", com("Vendas por vendedor", p, false, new Aba("Vendedores", List.of(new Coluna("Vendedor", TEXTO), new Coluna("Vendas", NUMERO), new Coluna("Total vendido", MOEDA),
                new Coluna("Ticket médio", MOEDA), new Coluna("Descontos", MOEDA)),
                analitico.porVendedor(p[0], p[1]).stream().map(v -> linha(v.vendedor(), v.vendas(), v.total(), v.ticketMedio(), v.descontos())).toList())));
    }
}

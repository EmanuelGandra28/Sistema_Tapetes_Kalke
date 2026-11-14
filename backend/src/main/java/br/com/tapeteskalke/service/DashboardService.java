package br.com.tapeteskalke.service;

import br.com.tapeteskalke.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resumo da tela inicial. Cada bloco só é montado se o cargo de quem está logado pode ver aquela área
 * (ex.: o entregador não vê faturamento, o vendedor não vê ordens de produção).
 */
@Service
public class DashboardService {
    private final VendaRepository vendas;
    private final ClienteRepository clientes;
    private final ProdutoRepository produtos;
    private final OrdemProducaoRepository ordens;
    private final EntregaRepository entregas;
    private final ProdutoComponenteRepository fichas;
    private final PermissionService permissions;
    private final Clock clock;

    public DashboardService(VendaRepository vendas, ClienteRepository clientes, ProdutoRepository produtos, OrdemProducaoRepository ordens, EntregaRepository entregas, ProdutoComponenteRepository fichas, PermissionService permissions, Clock clock) {
        this.vendas = vendas;
        this.clientes = clientes;
        this.produtos = produtos;
        this.ordens = ordens;
        this.entregas = entregas;
        this.fichas = fichas;
        this.permissions = permissions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        var paginas = permissions.pages();
        var acoes = permissions.actions();
        var hoje = LocalDate.now(clock);
        var inicioMes = hoje.withDayOfMonth(1);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hoje", hoje);

        if (Boolean.TRUE.equals(paginas.get("vendas")) || Boolean.TRUE.equals(acoes.get("relatorios_visualizar"))) {
            BigDecimal mes = vendas.totalFinalized(inicioMes, hoje);
            long qtdMes = vendas.countFinalized(inicioMes, hoje);
            // Mesmo intervalo do mês anterior (dia 1 até o mesmo dia), para a comparação ser justa no meio do mês.
            var inicioAnterior = inicioMes.minusMonths(1);
            var fimAnterior = inicioAnterior.withDayOfMonth(Math.min(hoje.getDayOfMonth(), inicioAnterior.lengthOfMonth()));
            BigDecimal anterior = vendas.totalFinalized(inicioAnterior, fimAnterior);
            BigDecimal cmv = vendas.costOfGoodsSold(inicioMes, hoje).setScale(2, RoundingMode.HALF_UP);
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("vendasHoje", vendas.totalFinalized(hoje, hoje));
            f.put("qtdVendasHoje", vendas.countFinalized(hoje, hoje));
            f.put("vendasMes", mes);
            f.put("qtdVendasMes", qtdMes);
            f.put("vendasMesAnteriorMesmoPeriodo", anterior);
            f.put("variacaoMes", anterior.signum() == 0 ? null : mes.subtract(anterior).multiply(BigDecimal.valueOf(100)).divide(anterior, 1, RoundingMode.HALF_UP));
            f.put("ticketMedioMes", qtdMes == 0 ? BigDecimal.ZERO : mes.divide(BigDecimal.valueOf(qtdMes), 2, RoundingMode.HALF_UP));
            f.put("lucroBrutoMes", mes.subtract(cmv));
            f.put("vendasPorDia", vendasPorDia(hoje.minusDays(29), hoje));
            f.put("maisVendidos", vendas.topProdutos(inicioMes, hoje, PageRequest.of(0, 5)).stream()
                    .map(r -> Map.of("produto", r[0], "quantidade", r[1], "total", r[2])).toList());
            m.put("vendas", f);
        }

        if (Boolean.TRUE.equals(paginas.get("clientes"))) {
            m.put("clientes", Map.of("total", clientes.countByDeletedAtIsNull(), "paraReativar", clientes.countParaReativar(hoje.minusMonths(2))));
        }

        if (Boolean.TRUE.equals(paginas.get("produtos")) || Boolean.TRUE.equals(paginas.get("producao"))) {
            var comFicha = fichas.produtosComFicha();
            m.put("estoqueBaixo", produtos.lowStock().stream().map(p -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("nome", p.getNome());
                item.put("comercial", p.getEstoqueComercial());
                item.put("minimoComercial", p.getEstoqueMinimoComercial());
                item.put("producao", p.getEstoqueProducao());
                item.put("minimoProducao", p.getEstoqueMinimoProducao());
                item.put("esgotado", p.getEstoqueComercial().signum() == 0 && p.isPodeSerVendido());
                // Produto vendido sem ficha técnica não ganha ordem automática: o painel avisa.
                item.put("semFichaTecnica", p.isPodeSerVendido() && !comFicha.contains(p.getId()));
                return item;
            }).toList());
        }

        if (Boolean.TRUE.equals(paginas.get("producao"))) {
            var abertas = ordens.abertas();
            m.put("producao", Map.of(
                    "abertas", abertas.size(),
                    "automaticas", abertas.stream().filter(o -> "AUTOMATICA".equals(o.getOrigem())).count(),
                    "ultimas", abertas.stream().limit(6).map(o -> Map.of("id", o.getId(), "numero", o.getNumeroOrdem(), "produto", o.getProdutoAcabado().getNome(),
                            "quantidade", o.getQuantidadeProduzida(), "origem", o.getOrigem(), "data", o.getDataOrdem())).toList()));
        }

        if (Boolean.TRUE.equals(paginas.get("entregas"))) {
            m.put("entregasHoje", entregas.doDia(hoje).stream().map(e -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", e.getId());
                item.put("numeroVenda", e.getVenda().getNumeroVenda());
                item.put("cliente", e.getCliente().getNome());
                item.put("bairro", e.getBairro());
                item.put("horario", e.getHorario());
                item.put("status", e.getStatus());
                return item;
            }).toList());
        }
        return m;
    }

    /** Total vendido por dia, com zero nos dias sem venda, para o gráfico não ter buracos. */
    private List<Map<String, Object>> vendasPorDia(LocalDate de, LocalDate ate) {
        Map<LocalDate, BigDecimal> totais = new HashMap<>();
        for (Object[] r : vendas.dailyTotals(de, ate)) totais.put((LocalDate) r[0], (BigDecimal) r[1]);
        List<Map<String, Object>> dias = new ArrayList<>();
        for (var d = de; !d.isAfter(ate); d = d.plusDays(1)) dias.add(Map.of("data", d, "total", totais.getOrDefault(d, BigDecimal.ZERO)));
        return dias;
    }
}

package br.com.tapeteskalke.service;

import br.com.tapeteskalke.repository.VendaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Resumo do período para a tela de Relatórios: indicadores com comparação ao período anterior (mesmo número de dias, logo antes)
// e os dados dos gráficos (vendas por dia, por forma de pagamento e produtos mais vendidos).
@Service
public class RelatorioResumoService {
    public record Indicadores(BigDecimal receita, BigDecimal lucroBruto, BigDecimal lucroLiquido, BigDecimal despesas, long vendas, BigDecimal ticketMedio) {}
    public record Dia(LocalDate data, BigDecimal total) {}
    public record Forma(String forma, long vendas, BigDecimal total) {}
    public record Produto(String produto, BigDecimal quantidade, BigDecimal receita) {}
    public record Resumo(LocalDate inicio, LocalDate fim, LocalDate inicioAnterior, LocalDate fimAnterior, Indicadores atual, Indicadores anterior,
                         List<Dia> porDia, List<Forma> porForma, List<Produto> maisVendidos) {}

    private final VendaRepository vendas;
    private final RelatorioService financeiro;

    public RelatorioResumoService(VendaRepository vendas, RelatorioService financeiro) {
        this.vendas = vendas;
        this.financeiro = financeiro;
    }

    /** Período imediatamente anterior, com o mesmo número de dias: [início, fim]. */
    public static LocalDate[] periodoAnterior(LocalDate inicio, LocalDate fim) {
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        var fimAnterior = inicio.minusDays(1);
        return new LocalDate[]{fimAnterior.minusDays(dias - 1), fimAnterior};
    }

    /** Um valor por dia do período, com zero nos dias sem venda (o gráfico não fica com buracos). */
    public static List<Dia> preencherDias(LocalDate de, LocalDate ate, Map<LocalDate, BigDecimal> totais) {
        var dias = new ArrayList<Dia>();
        for (var d = de; !d.isAfter(ate); d = d.plusDays(1)) dias.add(new Dia(d, totais.getOrDefault(d, BigDecimal.ZERO)));
        return dias;
    }

    @Transactional(readOnly = true)
    public Resumo resumo(LocalDate inicio, LocalDate fim) {
        var anterior = periodoAnterior(inicio, fim);
        var totais = new HashMap<LocalDate, BigDecimal>();
        for (var r : vendas.dailyTotals(inicio, fim)) totais.put((LocalDate) r[0], (BigDecimal) r[1]);
        var formas = vendas.totaisPorForma(inicio, fim).stream().map(r -> new Forma((String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2])).toList();
        var topo = vendas.topProdutos(inicio, fim, PageRequest.of(0, 5)).stream().map(r -> new Produto((String) r[0], (BigDecimal) r[1], (BigDecimal) r[2])).toList();
        return new Resumo(inicio, fim, anterior[0], anterior[1], indicadores(inicio, fim), indicadores(anterior[0], anterior[1]), preencherDias(inicio, fim, totais), formas, topo);
    }

    private Indicadores indicadores(LocalDate de, LocalDate ate) {
        var f = financeiro.financial(de, ate);
        long qtd = vendas.countFinalized(de, ate);
        var receita = f.get("receita");
        var ticket = qtd == 0 ? BigDecimal.ZERO : receita.divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_UP);
        return new Indicadores(receita, f.get("lucroBruto"), f.get("lucroLiquido"), f.get("salarios").add(f.get("outrasDespesas")), qtd, ticket);
    }
}

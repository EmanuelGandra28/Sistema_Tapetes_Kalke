package br.com.tapeteskalke.service;

import br.com.tapeteskalke.repository.VendaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Relatórios por produto (curva ABC e margem) e por vendedor. Só vendas FINALIZADAS.
// A receita de cada item é líquida do desconto da venda, rateado pelo valor do item, para somar igual à "Receita" do financeiro.
@Service
public class RelatorioAnaliticoService {
    public record AbcItem(String produto, BigDecimal quantidade, BigDecimal receita, BigDecimal percentual, BigDecimal acumulado, String classe) {}
    public record MargemItem(String produto, BigDecimal quantidade, BigDecimal receita, BigDecimal custo, BigDecimal margem, BigDecimal margemPercentual, boolean custoEstimado) {}
    public record VendedorItem(String vendedor, long vendas, BigDecimal total, BigDecimal ticketMedio, BigDecimal descontos) {}

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private final VendaRepository vendas;

    public RelatorioAnaliticoService(VendaRepository vendas) {
        this.vendas = vendas;
    }

    // Soma por produto: quantidade, receita líquida, custo (gravado ou, na falta, o atual) e se algum custo foi estimado.
    private static final class Acumulado {
        String nome; BigDecimal quantidade = BigDecimal.ZERO, receita = BigDecimal.ZERO, custo = BigDecimal.ZERO; boolean estimado;
    }

    private Collection<Acumulado> porProduto(LocalDate de, LocalDate ate) {
        var mapa = new LinkedHashMap<String, Acumulado>();
        for (var r : vendas.itensVendidos(de, ate)) {
            var a = mapa.computeIfAbsent((String) r[0], k -> new Acumulado());
            a.nome = (String) r[1];
            var qtd = (BigDecimal) r[2];
            var subtotalItem = (BigDecimal) r[3];
            var custoGravado = (BigDecimal) r[4];
            var subtotalVenda = (BigDecimal) r[5];
            var totalVenda = (BigDecimal) r[6];
            var custoAtual = r[7] == null ? BigDecimal.ZERO : (BigDecimal) r[7];
            var receita = subtotalVenda.signum() == 0 ? BigDecimal.ZERO : subtotalItem.multiply(totalVenda).divide(subtotalVenda, 6, RoundingMode.HALF_UP);
            a.quantidade = a.quantidade.add(qtd);
            a.receita = a.receita.add(receita);
            a.custo = a.custo.add(qtd.multiply(custoGravado != null ? custoGravado : custoAtual));
            if (custoGravado == null) a.estimado = true;
        }
        return mapa.values();
    }

    private static BigDecimal reais(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal pct(BigDecimal parte, BigDecimal todo) { return todo.signum() == 0 ? BigDecimal.ZERO.setScale(2) : parte.multiply(CEM).divide(todo, 2, RoundingMode.HALF_UP); }

    @Transactional(readOnly = true)
    public List<AbcItem> curvaAbc(LocalDate de, LocalDate ate) {
        var produtos = new ArrayList<>(porProduto(de, ate));
        produtos.sort(Comparator.comparing((Acumulado a) -> a.receita).reversed());
        var total = produtos.stream().map(a -> a.receita).reduce(BigDecimal.ZERO, BigDecimal::add);
        var lista = new ArrayList<AbcItem>();
        var acumulado = BigDecimal.ZERO;
        for (var a : produtos) {
            // A classe vem do acumulado ANTES do item: o produto que cruza 80% ainda é A (um produto sozinho com 100% é A, não C).
            var antes = pct(acumulado, total);
            var classe = antes.compareTo(BigDecimal.valueOf(80)) < 0 ? "A" : antes.compareTo(BigDecimal.valueOf(95)) < 0 ? "B" : "C";
            acumulado = acumulado.add(a.receita);
            var pctAcumulado = pct(acumulado, total);
            lista.add(new AbcItem(a.nome, a.quantidade, reais(a.receita), pct(a.receita, total), pctAcumulado, classe));
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public List<MargemItem> margem(LocalDate de, LocalDate ate) {
        return porProduto(de, ate).stream().map(a -> {
            var receita = reais(a.receita);
            var custo = reais(a.custo);
            var margem = receita.subtract(custo);
            return new MargemItem(a.nome, a.quantidade, receita, custo, margem, pct(margem, receita), a.estimado);
        }).sorted(Comparator.comparing(MargemItem::margem).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<VendedorItem> porVendedor(LocalDate de, LocalDate ate) {
        return vendas.vendasPorVendedor(de, ate).stream().map(r -> {
            var qtd = ((Number) r[1]).longValue();
            var total = (BigDecimal) r[2];
            var ticket = qtd == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_UP);
            return new VendedorItem(r[0] == null ? "Sem vendedor" : (String) r[0], qtd, total, ticket, (BigDecimal) r[3]);
        }).sorted(Comparator.comparing(VendedorItem::total).reversed()).toList();
    }
}

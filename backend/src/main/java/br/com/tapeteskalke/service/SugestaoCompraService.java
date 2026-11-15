package br.com.tapeteskalke.service;

import br.com.tapeteskalke.repository.OrdemProducaoRepository;
import br.com.tapeteskalke.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// O que comprar de matéria-prima: o que as ordens abertas vão consumir + o estoque mínimo de produção − o saldo atual.
@Service
public class SugestaoCompraService {
    public record SugestaoItem(String produtoId, String nome, String unidade, BigDecimal saldo, BigDecimal comprometido, BigDecimal minimo,
                               BigDecimal falta, String fornecedor, BigDecimal custoUnitario, BigDecimal custoEstimado) {}

    private final ProdutoRepository produtos;
    private final OrdemProducaoRepository ordens;

    public SugestaoCompraService(ProdutoRepository produtos, OrdemProducaoRepository ordens) {
        this.produtos = produtos;
        this.ordens = ordens;
    }

    @Transactional(readOnly = true)
    public List<SugestaoItem> calcular() {
        var comprometido = new HashMap<String, BigDecimal>();
        for (var linha : ordens.comprometidoPorMateria()) comprometido.put((String) linha[0], (BigDecimal) linha[1]);
        var lista = new ArrayList<SugestaoItem>();
        for (var p : produtos.materiasPrimasAtivas()) {
            var usado = comprometido.getOrDefault(p.getId(), BigDecimal.ZERO);
            var falta = usado.add(p.getEstoqueMinimoProducao()).subtract(p.getEstoqueProducao());
            if (falta.signum() <= 0) continue;
            var fornecedor = p.getFornecedorPrincipal() == null ? null : p.getFornecedorPrincipal().getNomeEmpresa();
            lista.add(new SugestaoItem(p.getId(), p.getNome(), p.getUnidadeMedida(), p.getEstoqueProducao(), usado, p.getEstoqueMinimoProducao(),
                    falta, fornecedor, p.getPrecoCusto(), falta.multiply(p.getPrecoCusto()).setScale(2, java.math.RoundingMode.HALF_UP)));
        }
        lista.sort(Comparator.comparing(SugestaoItem::custoEstimado).reversed().thenComparing(SugestaoItem::nome));
        return lista;
    }
}

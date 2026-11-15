package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.Fornecedor;
import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.repository.OrdemProducaoRepository;
import br.com.tapeteskalke.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SugestaoCompraServiceTest {
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final OrdemProducaoRepository ordens = mock(OrdemProducaoRepository.class);
    private final SugestaoCompraService service = new SugestaoCompraService(produtos, ordens);

    private Produto materia(String id, String nome, String saldo, String minimo, String custo) {
        var p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setUnidadeMedida("KG");
        p.setEstoqueProducao(new BigDecimal(saldo));
        p.setEstoqueMinimoProducao(new BigDecimal(minimo));
        p.setPrecoCusto(new BigDecimal(custo));
        return p;
    }

    @Test
    void listaSoOQueFaltaSomandoOrdensAbertasEMinimo() {
        var malha = materia("m1", "Malha", "10", "5", "12.50");
        var fornecedor = new Fornecedor();
        fornecedor.setNomeEmpresa("Fios SA");
        malha.setFornecedorPrincipal(fornecedor);
        var linha = materia("m2", "Linha", "20", "5", "3.00");
        var barbante = materia("m3", "Barbante", "0", "2", "1.00");
        when(produtos.materiasPrimasAtivas()).thenReturn(List.of(linha, malha, barbante));
        when(ordens.comprometidoPorMateria()).thenReturn(List.<Object[]>of(new Object[]{"m1", new BigDecimal("8")}, new Object[]{"m2", new BigDecimal("2")}));

        var lista = service.calcular();

        assertEquals(2, lista.size());
        var primeiro = lista.get(0);
        assertEquals("Malha", primeiro.nome());
        assertEquals(new BigDecimal("8"), primeiro.comprometido());
        assertEquals(0, new BigDecimal("3").compareTo(primeiro.falta()));
        assertEquals(0, new BigDecimal("37.50").compareTo(primeiro.custoEstimado()));
        assertEquals("Fios SA", primeiro.fornecedor());
        assertEquals("Barbante", lista.get(1).nome());
        assertEquals(0, new BigDecimal("2").compareTo(lista.get(1).falta()));
    }
}

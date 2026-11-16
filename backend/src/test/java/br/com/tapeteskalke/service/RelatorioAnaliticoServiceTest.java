package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelatorioAnaliticoServiceTest {
    private final VendaRepository vendas = mock(VendaRepository.class);
    private final RelatorioAnaliticoService service = new RelatorioAnaliticoService(vendas);
    private final LocalDate de = LocalDate.of(2026, 11, 1), ate = LocalDate.of(2026, 11, 30);

    // Linha por item vendido: produtoId, nome, quantidade, subtotal do item, custo gravado (pode ser nulo), subtotal da venda, total da venda, custo atual.
    private Object[] item(String id, String subtotal, String custoGravado, String custoAtual) {
        return new Object[]{id, "Produto " + id, BigDecimal.ONE, new BigDecimal(subtotal), custoGravado == null ? null : new BigDecimal(custoGravado), new BigDecimal(subtotal), new BigDecimal(subtotal), new BigDecimal(custoAtual)};
    }

    @Test
    void curvaAbcClassificaPeloAcumulado() {
        when(vendas.itensVendidos(any(), any())).thenReturn(List.of(item("b", "15", "0", "0"), item("a", "70", "0", "0"), item("d", "5", "0", "0"), item("c", "10", "0", "0")));
        var abc = service.curvaAbc(de, ate);
        assertEquals(List.of("Produto a", "Produto b", "Produto c", "Produto d"), abc.stream().map(RelatorioAnaliticoService.AbcItem::produto).toList());
        // A classe vem do acumulado ANTES do item: quem cruza a fronteira dos 80% (ou 95%) ainda fica na classe de cima.
        assertEquals(List.of("A", "A", "B", "C"), abc.stream().map(RelatorioAnaliticoService.AbcItem::classe).toList());
        assertEquals(0, new BigDecimal("85.00").compareTo(abc.get(1).acumulado()));
    }

    @Test
    void margemUsaCustoAtualQuandoOItemNaoTemCustoGravado() {
        when(vendas.itensVendidos(any(), any())).thenReturn(List.of(item("a", "100", "50", "30"), item("a", "100", null, "30"), item("b", "100", "40", "99")));
        var margem = service.margem(de, ate);
        var a = margem.stream().filter(m -> m.produto().equals("Produto a")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("80").compareTo(a.custo()));
        assertTrue(a.custoEstimado());
        assertEquals(0, new BigDecimal("120").compareTo(a.margem()));
        assertEquals(0, new BigDecimal("60.00").compareTo(a.margemPercentual()));
        var b = margem.stream().filter(m -> m.produto().equals("Produto b")).findFirst().orElseThrow();
        assertFalse(b.custoEstimado());
        assertEquals(0, new BigDecimal("40").compareTo(b.custo()));
    }

    @Test
    void vendasPorVendedorCalculaTicketMedio() {
        when(vendas.vendasPorVendedor(any(), any())).thenReturn(List.<Object[]>of(new Object[]{null, 2L, new BigDecimal("300.00"), new BigDecimal("10.00")}, new Object[]{"Joana", 3L, new BigDecimal("900.00"), BigDecimal.ZERO}));
        var lista = service.porVendedor(de, ate);
        assertEquals("Joana", lista.get(0).vendedor());
        assertEquals(0, new BigDecimal("300.00").compareTo(lista.get(0).ticketMedio()));
        assertEquals("Sem vendedor", lista.get(1).vendedor());
        assertEquals(0, new BigDecimal("150.00").compareTo(lista.get(1).ticketMedio()));
    }

    @Test
    void receitaDoProdutoDescontaODescontoDaVendaProporcionalmente() {
        // Venda de 200 com desconto de 20 (total 180): cada item de 100 vale 90 de receita.
        Object[] i1 = {"a", "Produto a", BigDecimal.ONE, new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("200"), new BigDecimal("180"), BigDecimal.TEN};
        Object[] i2 = {"b", "Produto b", BigDecimal.ONE, new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("200"), new BigDecimal("180"), BigDecimal.TEN};
        when(vendas.itensVendidos(any(), any())).thenReturn(List.of(i1, i2));
        var margem = service.margem(de, ate);
        assertEquals(0, new BigDecimal("180").compareTo(margem.stream().map(RelatorioAnaliticoService.MargemItem::receita).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void produtoUnicoEhClasseA() {
        when(vendas.itensVendidos(any(), any())).thenReturn(List.<Object[]>of(item("a", "190", "40", "40")));
        assertEquals("A", service.curvaAbc(de, ate).get(0).classe());
    }
}

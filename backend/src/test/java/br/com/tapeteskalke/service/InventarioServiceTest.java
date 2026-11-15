package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.dto.OperacaoDtos.InventarioItem;
import br.com.tapeteskalke.dto.OperacaoDtos.InventarioRequest;
import br.com.tapeteskalke.entity.Produto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InventarioServiceTest {
    private final ProdutoService produtos = mock(ProdutoService.class);
    private final EstoqueService estoque = mock(EstoqueService.class);
    private final InventarioService service = new InventarioService(produtos, estoque);

    private Produto produto(String id, String nome, String comercial) {
        var p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setEstoqueComercial(new BigDecimal(comercial));
        p.setEstoqueProducao(BigDecimal.ZERO);
        when(produtos.locked(id)).thenReturn(p);
        return p;
    }

    @Test
    void lancaSoAsDiferencasComOMesmoIdDeContagem() {
        var a = produto("p1", "Tapete A", "5");
        var b = produto("p2", "Tapete B", "4");

        var resumo = service.aplicar(new InventarioRequest("COMERCIAL", "Contagem de novembro",
                List.of(new InventarioItem("p1", new BigDecimal("3")), new InventarioItem("p2", new BigDecimal("4")))));

        var origem = ArgumentCaptor.forClass(String.class);
        verify(estoque).change(eq(a), eq("COMERCIAL"), eq(new BigDecimal("-2")), eq("INVENTARIO"), eq("INVENTARIO"), origem.capture(), eq("Contagem de novembro"));
        verify(estoque, never()).change(eq(b), anyString(), any(), anyString(), anyString(), anyString(), any());
        assertNotNull(origem.getValue());
        assertEquals(2, resumo.contados());
        assertEquals(1, resumo.comDiferenca());
        assertEquals("Tapete A", resumo.diferencas().get(0).produto());
        assertEquals(new BigDecimal("-2"), resumo.diferencas().get(0).diferenca());
    }

    @Test
    void usaOSaldoDeProducaoQuandoPedido() {
        var p = produto("p3", "Malha", "0");
        p.setEstoqueProducao(new BigDecimal("10"));

        service.aplicar(new InventarioRequest("PRODUCAO", null, List.of(new InventarioItem("p3", new BigDecimal("12.5")))));

        verify(estoque).change(eq(p), eq("PRODUCAO"), eq(new BigDecimal("2.5")), eq("INVENTARIO"), eq("INVENTARIO"), anyString(), eq("Inventário"));
    }
}

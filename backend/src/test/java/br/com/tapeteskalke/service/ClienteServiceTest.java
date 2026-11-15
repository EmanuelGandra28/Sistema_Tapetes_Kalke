package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.Cliente;
import br.com.tapeteskalke.entity.Venda;
import br.com.tapeteskalke.entity.VendaItem;
import br.com.tapeteskalke.repository.ClienteRepository;
import br.com.tapeteskalke.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClienteServiceTest {
    private final ClienteRepository repo = mock(ClienteRepository.class);
    private final VendaRepository vendas = mock(VendaRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final ClienteService service = new ClienteService(repo, mock(AuditService.class), clock, vendas);

    private Venda venda(long numero, String data, String total, String status) {
        var v = new Venda();
        v.setId("v" + numero);
        v.setNumeroVenda(numero);
        v.setDataVenda(LocalDate.parse(data));
        v.setTotalVenda(new BigDecimal(total));
        v.setDesconto(BigDecimal.ZERO);
        v.setFormaPagamento("PIX");
        v.setStatus(status);
        var item = new VendaItem();
        item.setProdutoNomeSnapshot("Tapete");
        item.setQuantidade(BigDecimal.ONE);
        item.setPrecoUnitario(new BigDecimal(total));
        item.setSubtotal(new BigDecimal(total));
        v.setItens(List.of(item));
        return v;
    }

    @Test
    void historicoSomaSoAsVendasFinalizadasMasListaTodas() {
        var c = new Cliente();
        c.setId("c1");
        c.setNome("Ana");
        c.setStatusCadastral("ATIVO");
        c.setTotalCompras(BigDecimal.ZERO);
        when(repo.activeById("c1")).thenReturn(Optional.of(c));
        when(vendas.historyByClient("c1")).thenReturn(List.of(
                venda(3, "2026-11-10", "30.00", "CANCELADA"),
                venda(2, "2026-11-05", "50.00", "FINALIZADA"),
                venda(1, "2026-10-01", "100.00", "FINALIZADA")));

        var h = service.historico("c1");

        assertEquals(new BigDecimal("150.00"), h.totalFinalizado());
        assertEquals(2, h.compras());
        assertEquals(new BigDecimal("75.00"), h.ticketMedio());
        assertEquals(LocalDate.parse("2026-11-05"), h.ultimaCompra());
        assertEquals(List.of(3L, 2L, 1L), h.vendas().stream().map(v -> v.numero()).toList());
        assertEquals("Tapete", h.vendas().get(0).itens().get(0).produto());
    }

    @Test
    void historicoSemComprasTemTicketZero() {
        var c = new Cliente();
        c.setId("c2");
        c.setNome("Bia");
        c.setStatusCadastral("ATIVO");
        c.setTotalCompras(BigDecimal.ZERO);
        when(repo.activeById("c2")).thenReturn(Optional.of(c));
        when(vendas.historyByClient("c2")).thenReturn(List.of());

        var h = service.historico("c2");

        assertEquals(0, h.compras());
        assertEquals(0, BigDecimal.ZERO.compareTo(h.ticketMedio()));
        assertNull(h.ultimaCompra());
    }
}

package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.dto.OperacaoDtos.VendaItem;
import br.com.tapeteskalke.dto.OperacaoDtos.VendaRequest;
import br.com.tapeteskalke.entity.Cliente;
import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.entity.Venda;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.*;
import br.com.tapeteskalke.security.CurrentUser;
import br.com.tapeteskalke.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VendaServiceTest {
    private final VendaRepository vendas = mock(VendaRepository.class);
    private final ClienteService clientes = mock(ClienteService.class);
    private final ProdutoService produtos = mock(ProdutoService.class);
    private final EstoqueService estoque = mock(EstoqueService.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final NumeroService numeros = mock(NumeroService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final VendaService service = new VendaService(vendas, clientes, produtos, mock(FuncionarioRepository.class), estoque,
            mock(PermissionService.class), current, numeros, clock, mock(EntregaRepository.class), mock(ClienteRepository.class), mock(CodigoService.class));
    private Produto tapete;

    @BeforeEach
    void setUp() {
        when(current.require()).thenReturn(new UserPrincipal("u1", "a@b.com", "VENDEDOR"));
        when(vendas.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        var cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        when(clientes.active("c1")).thenReturn(cliente);
        when(vendas.finalizedByClient("c1")).thenReturn(List.of());
        when(numeros.nextVenda()).thenReturn(10L);
        when(vendas.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
        tapete = new Produto();
        tapete.setId("p1");
        tapete.setNome("Tapete");
        tapete.setUnidadeMedida("UNIDADE");
        tapete.setPrecoVenda(new BigDecimal("99.90"));
        tapete.setPrecoCusto(new BigDecimal("40.00"));
        when(produtos.locked("p1")).thenReturn(tapete);
    }

    private VendaRequest venda(String desconto, String quantidade) {
        return new VendaRequest("c1", null, null, new BigDecimal(desconto), "PIX", List.of(new VendaItem("p1", new BigDecimal(quantidade))), null);
    }

    @Test
    void servidorCalculaTotalComPrecoDoCadastroEDataDaLoja() {
        var r = service.create(venda("9.80", "2"), "k1");
        assertEquals(new BigDecimal("199.80"), r.subtotal());
        assertEquals(new BigDecimal("190.00"), r.total());
        assertEquals(LocalDate.of(2026, 10, 27), r.dataVenda());
        verify(estoque).change(eq(tapete), eq("COMERCIAL"), eq(new BigDecimal("-2")), eq("SAIDA_VENDA"), eq("VENDA"), any(), any());
    }

    @Test
    void recusaProdutoSemPreco() {
        tapete.setPrecoVenda(BigDecimal.ZERO);
        var e = assertThrows(BusinessException.class, () -> service.create(venda("0", "1"), "k1"));
        assertEquals("PRODUTO_SEM_PRECO", e.code());
        verifyNoInteractions(estoque);
    }

    @Test
    void recusaDescontoMaiorQueSubtotal() {
        var e = assertThrows(BusinessException.class, () -> service.create(venda("100.00", "1"), "k1"));
        assertEquals("DESCONTO_INVALIDO", e.code());
    }

    @Test
    void chaveRepetidaDoMesmoUsuarioDevolveAVendaOriginal() {
        var original = new Venda();
        original.setId("v1");
        original.setCreatedBy("u1");
        original.setCliente(clientes.active("c1"));
        when(vendas.findByIdempotencyKey("k1")).thenReturn(Optional.of(original));
        assertEquals("v1", service.create(venda("0", "1"), "k1").id());
        verify(vendas, never()).save(any());
    }

    @Test
    void chaveDeOutroUsuarioGeraConflito() {
        var original = new Venda();
        original.setCreatedBy("outro");
        when(vendas.findByIdempotencyKey("k1")).thenReturn(Optional.of(original));
        var e = assertThrows(BusinessException.class, () -> service.create(venda("0", "1"), "k1"));
        assertEquals("IDEMPOTENCY_KEY_EM_USO", e.code());
    }
}

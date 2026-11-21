package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.dto.ProdutoDtos.Request;
import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.FornecedorRepository;
import br.com.tapeteskalke.repository.ProdutoRepository;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdutoPortfolioTest {
    private final ProdutoRepository repo = mock(ProdutoRepository.class);
    private final ProdutoService service = new ProdutoService(repo, mock(FornecedorRepository.class), mock(AuditService.class), mock(CodigoService.class));

    ProdutoPortfolioTest() {
        when(repo.save(any(Produto.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Request pedido(String modo, String linha, String descricao, Integer ordem, String selo) {
        return new Request("Conjunto Tulipas", "TAPETE", null, "Vermelho", "3 peças", "UNIDADE", true, false,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("119.90"), null, null, null, true,
                modo, linha, descricao, ordem, selo);
    }

    @Test
    void guardaOsCamposDePortfolioInformados() {
        var r = service.create(pedido("VENDA", "COZINHA", "Tulipas vermelhas", 3, "Novo"));
        assertEquals("VENDA", r.portfolioModo());
        assertEquals("COZINHA", r.portfolioLinha());
        assertEquals("Tulipas vermelhas", r.portfolioDescricao());
        assertEquals(3, r.portfolioOrdem());
        assertEquals("Novo", r.portfolioSelo());
    }

    @Test
    void camposOmitidosFicamForaDoSite() {
        var r = service.create(pedido(null, null, null, null, null));
        assertEquals("NENHUM", r.portfolioModo());
        assertNull(r.portfolioLinha());
        assertNull(r.portfolioDescricao());
        assertEquals(0, r.portfolioOrdem());
        assertNull(r.portfolioSelo());
    }

    @Test
    void recusaModoELinhaDesconhecidos() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(pedido("XYZ", null, null, null, null)).isEmpty());
        assertFalse(validator.validate(pedido("VENDA", "QUARTO", null, null, null)).isEmpty());
        assertTrue(validator.validate(pedido("GALERIA", "SALA", null, 0, null)).isEmpty());
    }

    @Test
    void tapeteAVendaNoSiteExigeALinha() {
        var e = assertThrows(BusinessException.class, () -> service.create(pedido("VENDA", null, null, null, null)));
        assertEquals("PORTFOLIO_LINHA", e.code());
        verify(repo, never()).save(any());
    }

    @Test
    void trabalhoDaGaleriaNaoPrecisaDeLinha() {
        assertEquals("GALERIA", service.create(pedido("GALERIA", null, null, null, null)).portfolioModo());
    }
}

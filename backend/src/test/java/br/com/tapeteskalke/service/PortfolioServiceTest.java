package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.repository.ProdutoRepository;
import br.com.tapeteskalke.service.ProdutoImagemService.ImagemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortfolioServiceTest {
    private final ProdutoRepository repo = mock(ProdutoRepository.class);
    private final ProdutoImagemService imagens = mock(ProdutoImagemService.class);
    private final PortfolioService service = new PortfolioService(repo, imagens, Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC));

    private static Produto produto(String id, String modo, String tipo, boolean ativo, String preco) {
        var p = new Produto();
        p.setId(id);
        p.setNome("Conjunto " + id);
        p.setTipo(tipo);
        p.setAtivo(ativo);
        p.setPortfolioModo(modo);
        p.setPortfolioLinha("COZINHA");
        p.setPortfolioDescricao("Descrição " + id);
        p.setCor("Vermelho");
        p.setTamanho("3 peças");
        p.setPrecoVenda(new BigDecimal(preco));
        p.setPrecoCusto(new BigDecimal("55.55"));
        p.setObservacoes("OBS-INTERNA");
        p.setCodigoBarras("789000111");
        p.setEstoqueComercial(new BigDecimal("42"));
        return p;
    }

    @Test
    void soPublicaTapeteAtivoNaoExcluidoEMarcadoParaOSite() {
        var vivo = produto("a", "VENDA", "TAPETE", true, "100.00");
        var excluido = produto("b", "VENDA", "TAPETE", true, "100.00");
        excluido.setDeletedAt(java.time.OffsetDateTime.now());
        when(repo.portfolio()).thenReturn(List.of(
                vivo,
                produto("c", "NENHUM", "TAPETE", true, "100.00"),
                produto("d", "VENDA", "TAPETE", false, "100.00"),
                produto("e", "VENDA", "MALHA", true, "100.00"),
                excluido));
        var ids = service.catalogo().produtos().stream().map(PortfolioService.ItemPublico::id).toList();
        assertEquals(List.of("a"), ids);
    }

    @Test
    void galeriaSaiSemPrecoEVendaComPrecoZeroTambem() {
        when(repo.portfolio()).thenReturn(List.of(
                produto("g", "GALERIA", "TAPETE", true, "150.00"),
                produto("v0", "VENDA", "TAPETE", true, "0.00"),
                produto("v1", "VENDA", "TAPETE", true, "119.90")));
        var itens = service.catalogo().produtos();
        assertNull(itens.get(0).preco());
        assertNull(itens.get(1).preco());
        assertEquals(new BigDecimal("119.90"), itens.get(2).preco());
    }

    @Test
    void incluiAsFotosDoProduto() {
        when(repo.portfolio()).thenReturn(List.of(produto("a", "VENDA", "TAPETE", true, "10.00")));
        when(imagens.listar("a")).thenReturn(List.of(new ImagemResponse("f1", true, 0), new ImagemResponse("f2", false, 1)));
        var fotos = service.catalogo().produtos().get(0).fotos();
        assertEquals(List.of("f1", "f2"), fotos.stream().map(ImagemResponse::id).toList());
    }

    @Test
    void oJsonNaoVazaDadosInternos() throws Exception {
        when(repo.portfolio()).thenReturn(List.of(produto("a", "VENDA", "TAPETE", true, "10.00")));
        var json = new ObjectMapper().writeValueAsString(service.catalogo());
        for (var proibido : List.of("precoCusto", "55.55", "estoque", "OBS-INTERNA", "observacoes", "789000111", "codigoBarras", "fornecedor")) {
            assertFalse(json.contains(proibido), "vazou: " + proibido);
        }
        assertTrue(json.contains("\"geradoEm\":\"2026-11-21T12:00:00Z\""));
    }
}

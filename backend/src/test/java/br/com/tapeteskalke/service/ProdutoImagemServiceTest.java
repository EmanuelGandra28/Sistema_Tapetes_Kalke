package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.ProdutoImagem;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.ProdutoImagemRepository;
import br.com.tapeteskalke.repository.ProdutoRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProdutoImagemServiceTest {
    private final ProdutoImagemRepository repo = mock(ProdutoImagemRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final ProdutoImagemService service = new ProdutoImagemService(repo, produtos, mock(AuditService.class));

    private static byte[] jpeg(int tamanho) {
        var b = new byte[tamanho];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        return b;
    }

    @Test
    void reconheceOTipoPeloConteudoENaoPeloNome() {
        assertEquals("image/jpeg", ProdutoImagemService.detectarTipo(jpeg(20)));
        var png = new byte[20];
        png[0] = (byte) 0x89; png[1] = 'P'; png[2] = 'N'; png[3] = 'G';
        assertEquals("image/png", ProdutoImagemService.detectarTipo(png));
        var webp = new byte[20];
        webp[0] = 'R'; webp[1] = 'I'; webp[2] = 'F'; webp[3] = 'F'; webp[8] = 'W'; webp[9] = 'E'; webp[10] = 'B'; webp[11] = 'P';
        assertEquals("image/webp", ProdutoImagemService.detectarTipo(webp));
        assertNull(ProdutoImagemService.detectarTipo("<script>alert(1)</script>".getBytes()));
        assertNull(ProdutoImagemService.detectarTipo(null));
    }

    @Test
    void primeiraFotoViraPrincipalESegundaNao() {
        when(produtos.existsById("p1")).thenReturn(true);
        when(repo.save(any(ProdutoImagem.class))).thenAnswer(i -> { ProdutoImagem x = i.getArgument(0); x.setId("i" + x.getOrdem()); return x; });
        when(repo.contar("p1")).thenReturn(0L);
        assertTrue(service.adicionar("p1", jpeg(100)).principal());
        when(repo.contar("p1")).thenReturn(1L);
        assertFalse(service.adicionar("p1", jpeg(100)).principal());
    }

    @Test
    void recusaArquivoQueNaoEImagemGrandeDemaisOuExcessoDeFotos() {
        when(produtos.existsById("p1")).thenReturn(true);
        when(repo.contar("p1")).thenReturn(0L);
        assertEquals("IMAGEM_INVALIDA", assertThrows(BusinessException.class, () -> service.adicionar("p1", "texto qualquer longo".getBytes())).code());
        assertEquals("IMAGEM_GRANDE", assertThrows(BusinessException.class, () -> service.adicionar("p1", jpeg(ProdutoImagemService.TAMANHO_MAXIMO + 1))).code());
        when(repo.contar("p1")).thenReturn((long) ProdutoImagemService.MAXIMO_POR_PRODUTO);
        assertEquals("LIMITE_IMAGENS", assertThrows(BusinessException.class, () -> service.adicionar("p1", jpeg(100))).code());
        verify(repo, never()).save(any());
    }

    @Test
    void removerAPrincipalPassaAPrincipalParaAProximaFoto() {
        var principal = new ProdutoImagem();
        principal.setId("a");
        principal.setPrincipal(true);
        var outra = new ProdutoImagem();
        outra.setId("b");
        when(repo.doProduto("p1", "a")).thenReturn(Optional.of(principal));
        when(repo.todas("p1")).thenReturn(List.of(outra));
        service.remover("p1", "a");
        verify(repo).delete(principal);
        assertTrue(outra.isPrincipal());
    }

    @Test
    void produtoInexistenteNaoRecebeFoto() {
        when(produtos.existsById("x")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.adicionar("x", jpeg(50)));
        assertNotNull(Arrays.asList(1));
    }
}

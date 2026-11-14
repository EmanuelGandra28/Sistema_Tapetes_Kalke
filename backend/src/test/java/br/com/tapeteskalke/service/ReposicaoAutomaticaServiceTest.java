package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.OrdemProducao;
import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.entity.ProdutoComponente;
import br.com.tapeteskalke.repository.OrdemProducaoRepository;
import br.com.tapeteskalke.repository.ProdutoComponenteRepository;
import br.com.tapeteskalke.security.CurrentUser;
import br.com.tapeteskalke.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ReposicaoAutomaticaServiceTest {
    private final ProdutoService produtos = mock(ProdutoService.class);
    private final ProdutoComponenteRepository fichas = mock(ProdutoComponenteRepository.class);
    private final OrdemProducaoRepository ordens = mock(OrdemProducaoRepository.class);
    private final NumeroService numeros = mock(NumeroService.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final ReposicaoAutomaticaService service = new ReposicaoAutomaticaService(produtos, fichas, ordens, numeros, current, eventos, clock);
    private Produto tapete;
    private Produto malha;

    @BeforeEach
    void setUp() {
        tapete = new Produto();
        tapete.setId("t1");
        tapete.setNome("Tapete Sala");
        tapete.setUnidadeMedida("UNIDADE");
        tapete.setEstoqueComercial(new BigDecimal("1"));
        tapete.setEstoqueMinimoComercial(new BigDecimal("3"));
        malha = new Produto();
        malha.setId("m1");
        malha.setNome("Malha");
        malha.setUnidadeMedida("METRO");
        when(produtos.locked("t1")).thenReturn(tapete);
        when(numeros.nextOrdem()).thenReturn(7L);
        when(current.require()).thenReturn(new UserPrincipal("u1", "a@b.com", "VENDEDOR"));
        var linha = new ProdutoComponente();
        linha.setProduto(tapete);
        linha.setMateriaPrima(malha);
        linha.setQuantidade(new BigDecimal("2.5"));
        when(fichas.fichaDe("t1")).thenReturn(List.of(linha));
    }

    @Test
    void abreOrdemComMateriasDaFichaParaVoltarAoDobroDoMinimo() {
        var ordem = service.criarSeNecessario("t1").orElseThrow();
        assertEquals("AUTOMATICA", ordem.getOrigem());
        assertEquals("ESTOQUE", ordem.getTipoOrdem());
        assertEquals(new BigDecimal("5"), ordem.getQuantidadeProduzida());
        assertEquals(new BigDecimal("12.500"), ordem.getMateriasPrimas().get(0).getQuantidadeUsada());
        assertEquals(LocalDate.of(2026, 11, 14), ordem.getDataOrdem());
        verify(ordens).save(ordem);
        verify(eventos).publishEvent(any(EventosEstoque.OrdemAutomaticaCriada.class));
    }

    @Test
    void naoDuplicaQuandoJaExisteOrdemAberta() {
        when(ordens.existsByProdutoAcabadoIdAndStatus("t1", "ABERTA")).thenReturn(true);
        assertTrue(service.criarSeNecessario("t1").isEmpty());
        verify(ordens, never()).save(any(OrdemProducao.class));
    }

    @Test
    void semFichaTecnicaNaoCriaOrdem() {
        when(fichas.fichaDe("t1")).thenReturn(List.of());
        assertTrue(service.criarSeNecessario("t1").isEmpty());
        verify(ordens, never()).save(any(OrdemProducao.class));
    }

    @Test
    void saldoJaRepostoNaoCriaOrdem() {
        tapete.setEstoqueComercial(new BigDecimal("4"));
        assertTrue(service.criarSeNecessario("t1").isEmpty());
        verify(ordens, never()).save(any(OrdemProducao.class));
    }

    @Test
    void produtoQueNaoEVendidoNaoGeraOrdem() {
        tapete.setPodeSerVendido(false);
        assertTrue(service.criarSeNecessario("t1").isEmpty());
    }

    @Test
    void quantidadeDeReposicao() {
        assertEquals(new BigDecimal("6"), ReposicaoAutomaticaService.quantidadeReposicao(BigDecimal.ZERO, new BigDecimal("3"), "UNIDADE"));
        assertEquals(new BigDecimal("2"), ReposicaoAutomaticaService.quantidadeReposicao(BigDecimal.ZERO, BigDecimal.ONE, "UNIDADE"));
        assertEquals(new BigDecimal("1"), ReposicaoAutomaticaService.quantidadeReposicao(BigDecimal.ZERO, BigDecimal.ZERO, "UNIDADE"));
        assertEquals(new BigDecimal("3"), ReposicaoAutomaticaService.quantidadeReposicao(new BigDecimal("1.5"), new BigDecimal("2"), "UNIDADE"));
        assertEquals(new BigDecimal("7.500"), ReposicaoAutomaticaService.quantidadeReposicao(new BigDecimal("2.5"), new BigDecimal("5"), "METRO"));
    }
}

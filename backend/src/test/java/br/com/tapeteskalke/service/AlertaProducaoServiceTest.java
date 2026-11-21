package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.*;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlertaProducaoServiceTest {
    private final AlertaProducaoRepository alertas = mock(AlertaProducaoRepository.class);
    private final ConfiguracaoLojaRepository configuracao = mock(ConfiguracaoLojaRepository.class);
    private final OrdemProducaoRepository ordens = mock(OrdemProducaoRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final ProdutoImagemRepository imagens = mock(ProdutoImagemRepository.class);
    private final WhatsappMensagemRepository mensagens = mock(WhatsappMensagemRepository.class);
    private final MensagemService modelos = mock(MensagemService.class);
    private final WhatsAppProvider provider = mock(WhatsAppProvider.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Sao_Paulo")); // 12h em São Paulo
    private AlertaProducaoService service = novo(clock, 500);
    private ConfiguracaoLoja loja;
    private AlertaProducao a1, a2;

    private AlertaProducaoService novo(Clock c, int cota) {
        return new AlertaProducaoService(alertas, configuracao, ordens, produtos, imagens, mensagens, modelos, provider, usuarios, audit, c, cota, "https://exemplo.github.io", "/Sistema_Tapetes_Kalke");
    }

    private AlertaProducao alerta(String id, String produtoId, String nome, String saldo, String minimo, String qtd) {
        var a = new AlertaProducao();
        a.setId(id);
        a.setIdOrdem("o-" + id);
        a.setIdProduto(produtoId);
        a.setProdutoNome(nome);
        a.setSaldo(new BigDecimal(saldo));
        a.setEstoqueMinimo(new BigDecimal(minimo));
        a.setQuantidade(new BigDecimal(qtd));
        return a;
    }

    @BeforeEach
    void cenario() {
        loja = new ConfiguracaoLoja();
        loja.setId(ConfiguracaoLoja.ID);
        loja.setWhatsappDona("+5511987654321");
        loja.setAlertaProducaoAtivo(true);
        loja.setAlertaEvidencia("A dona ativou o alerta em Configurações");
        a1 = alerta("a1", "p1", "Tapete Floral", "2.000", "5.000", "8.000");
        a2 = alerta("a2", "p2", "Tapete Liso", "0.000", "3.000", "6.000");
        var p1 = new Produto();
        p1.setId("p1");
        p1.setCor("Azul");
        p1.setTamanho("2x3");
        when(configuracao.findById(ConfiguracaoLoja.ID)).thenReturn(Optional.of(loja));
        when(provider.configurado()).thenReturn(true);
        when(alertas.findAllByStatusOrderByCreatedAtAsc("PENDENTE")).thenReturn(List.of(a1, a2));
        when(produtos.findById("p1")).thenReturn(Optional.of(p1));
        when(modelos.renderizar(eq("ALERTA_PRODUCAO_DONA"), any())).thenReturn(Optional.of("Atenção!"));
        when(provider.enviarTexto(any(), any(), any(), any(), any())).thenReturn(new WhatsAppProvider.Enviada("g1", "na_fila"));
        when(provider.enviarImagem(any(), any(), any(), any(), any(), any(), any())).thenReturn(new WhatsAppProvider.Enviada("g2", "na_fila"));
        when(imagens.principais()).thenReturn(List.of());
    }

    @Test
    void linhaEListaDosTapetes() {
        assertEquals("1) Tapete Floral (Azul, 2x3) — saldo 2, mínimo 5 → produzir 8", AlertaProducaoService.linha(1, "Tapete Floral", "Azul", "2x3", new BigDecimal("2.000"), new BigDecimal("5.000"), new BigDecimal("8.000")));
        assertEquals("2) Tapete Liso — saldo 0, mínimo 3 → produzir 6", AlertaProducaoService.linha(2, "Tapete Liso", null, " ", BigDecimal.ZERO, new BigDecimal("3"), new BigDecimal("6")));
        var doze = new ArrayList<String>();
        for (int i = 1; i <= 12; i++) doze.add(i + ") x");
        var lista = AlertaProducaoService.montarLista(doze, 10);
        assertTrue(lista.endsWith("...e mais 2 tapetes"));
        assertEquals(11, lista.split("\n").length);
        assertTrue(AlertaProducaoService.montarLista(doze.subList(0, 11), 10).endsWith("...e mais 1 tapete"));
        assertEquals("1) x", AlertaProducaoService.montarLista(List.of("1) x"), 10));
    }

    @Test
    void agrupaTodosOsPendentesEmUmaUnicaMensagemEMarcaComoEnviados() {
        var r = service.processar();
        assertEquals(2, r.alertas());
        assertEquals(1, r.mensagens());
        var dados = ArgumentCaptor.forClass(Map.class);
        verify(modelos).renderizar(eq("ALERTA_PRODUCAO_DONA"), dados.capture());
        var lista = (String) dados.getValue().get("lista_tapetes");
        assertTrue(lista.contains("1) Tapete Floral (Azul, 2x3) — saldo 2, mínimo 5 → produzir 8"));
        assertTrue(lista.contains("2) Tapete Liso"));
        assertEquals("https://exemplo.github.io/Sistema_Tapetes_Kalke/ordens-producao", dados.getValue().get("link"));
        verify(provider, times(1)).enviarTexto(eq("+5511987654321"), eq("Atenção!"), eq("aviso_operacional"), eq("alerta-producao-a1"), eq("A dona ativou o alerta em Configurações"));
        assertEquals("ENVIADO", a1.getStatus());
        assertEquals("ENVIADO", a2.getStatus());
        assertEquals("a1", a2.getLote());
    }

    @Test
    void enviaAteTresFotosDosTapetesQueTemFoto() {
        var principais = new ArrayList<ProdutoImagemRepository.Resumo>();
        for (var id : List.of("p1", "p2", "p3", "p4")) { var rs = resumo(id, "img-" + id); principais.add(rs); }
        when(imagens.principais()).thenReturn(principais);
        var alertas4 = List.of(a1, a2, alerta("a3", "p3", "Tapete 3", "1", "3", "5"), alerta("a4", "p4", "Tapete 4", "1", "3", "5"));
        when(alertas.findAllByStatusOrderByCreatedAtAsc("PENDENTE")).thenReturn(alertas4);
        for (var id : List.of("p1", "p2", "p3")) {
            var img = new ProdutoImagem();
            img.setDados(new byte[]{1, 2, 3});
            img.setContentType("image/webp");
            when(imagens.findById("img-" + id)).thenReturn(Optional.of(img));
        }
        var r = service.processar();
        assertEquals(4, r.mensagens()); // 1 texto + 3 fotos
        verify(provider, times(3)).enviarImagem(eq("+5511987654321"), any(), eq("image/webp"), anyString(), any(), anyString(), any());
        verify(provider, never()).enviarImagem(any(), any(), any(), eq("Tapete 4"), any(), any(), any());
    }

    @Test
    void falhaNaFotoNaoDesfazOAvisoPrincipal() {
        var umaFoto = resumo("p1", "img-p1");
        when(imagens.principais()).thenReturn(List.of(umaFoto));
        var img = new ProdutoImagem();
        img.setDados(new byte[]{1});
        img.setContentType("image/jpeg");
        when(imagens.findById("img-p1")).thenReturn(Optional.of(img));
        doThrow(new GatewayException(503, "ANTIVIRUS_INDISPONIVEL", "Tente de novo", null)).when(provider).enviarImagem(any(), any(), any(), any(), any(), any(), any());
        var r = service.processar();
        assertEquals(1, r.mensagens());
        assertEquals("ENVIADO", a1.getStatus());
    }

    @Test
    void foraDoHorarioOuDesligadoOuSemChaveNaoEnviaNada() {
        var noite = Clock.fixed(Instant.parse("2026-11-21T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        assertNotNull(novo(noite, 500).processar().motivo());
        loja.setAlertaProducaoAtivo(false);
        assertEquals("Alerta desligado", service.processar().motivo());
        loja.setAlertaProducaoAtivo(true);
        when(provider.configurado()).thenReturn(false);
        assertNotNull(service.processar().motivo());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
        assertEquals("PENDENTE", a1.getStatus());
    }

    @Test
    void respeitaACotaDoMes() {
        when(mensagens.countByEnviadoEmGreaterThanEqualAndEnviadoEmLessThan(any(), any())).thenReturn(450L);
        var r = service.processar();
        assertNotNull(r.motivo());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
        assertEquals("PENDENTE", a1.getStatus());
    }

    @Test
    void mensagemDesativadaFazOAvisoEsperar() {
        when(modelos.renderizar(eq("ALERTA_PRODUCAO_DONA"), any())).thenReturn(Optional.empty());
        assertNotNull(service.processar().motivo());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
        assertEquals("PENDENTE", a1.getStatus());
    }

    @Test
    void semSaldoEntraEmEsperaSemMarcarFalhaEErroDefinitivoMarcaFalha() {
        doThrow(new GatewayException(402, "LIMITE_DO_PLANO_ATINGIDO", "Limite", null)).when(provider).enviarTexto(any(), any(), any(), any(), any());
        service.processar();
        assertEquals("PENDENTE", a1.getStatus());
        assertEquals(1, a1.getTentativas());
        doThrow(new GatewayException(422, "CONSENTIMENTO_AUSENTE", "Sem consentimento", null)).when(provider).enviarTexto(any(), any(), any(), any(), any());
        service.processar();
        assertEquals("FALHOU", a1.getStatus());
        assertEquals("CONSENTIMENTO_AUSENTE", a1.getErroCodigo());
    }

    @Test
    void cincoTentativasSemSucessoMarcamFalha() {
        doThrow(new GatewayException(503, "GATEWAY_INDISPONIVEL", "Fora do ar", null)).when(provider).enviarTexto(any(), any(), any(), any(), any());
        for (int i = 0; i < 5; i++) service.processar();
        assertEquals("FALHOU", a1.getStatus());
    }

    @Test
    void mensagemJaEnviadaNaoSaiDeNovo() {
        when(mensagens.existsByIdempotencyKey("alerta-producao-a1")).thenReturn(true);
        service.processar();
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
        assertEquals("ENVIADO", a1.getStatus());
    }

    @Test
    void reenviarColocaAsFalhasNaFilaDeNovo() {
        a1.setStatus("FALHOU");
        a1.setTentativas(5);
        when(alertas.findAllByStatusOrderByCreatedAtAsc("FALHOU")).thenReturn(List.of(a1));
        assertEquals(1, service.reenviarFalhas());
        assertEquals("PENDENTE", a1.getStatus());
        assertEquals(0, a1.getTentativas());
    }

    @Test
    void ordemAutomaticaViraAlertaSoComOAlertaLigadoEUmaVezPorOrdem() {
        var ordem = new OrdemProducao();
        var produto = new Produto();
        produto.setId("p1");
        ordem.setProdutoAcabado(produto);
        when(ordens.findById("o9")).thenReturn(Optional.of(ordem));
        var evento = new EventosEstoque.OrdemAutomaticaCriada("o9", 9L, "Tapete Floral", new BigDecimal("8"), new BigDecimal("2"), new BigDecimal("5"));
        service.aoCriarOrdem(evento);
        var salvo = ArgumentCaptor.forClass(AlertaProducao.class);
        verify(alertas).save(salvo.capture());
        assertEquals("o9", salvo.getValue().getIdOrdem());
        assertEquals("p1", salvo.getValue().getIdProduto());
        when(alertas.existsByIdOrdem("o9")).thenReturn(true);
        service.aoCriarOrdem(evento);
        loja.setAlertaProducaoAtivo(false);
        when(alertas.existsByIdOrdem("o9")).thenReturn(false);
        service.aoCriarOrdem(evento);
        verify(alertas, times(1)).save(any());
    }

    @Test
    void falhaAoEnfileirarNuncaPropagaParaAVenda() {
        when(ordens.findById(any())).thenThrow(new RuntimeException("banco fora"));
        assertDoesNotThrow(() -> service.aoCriarOrdem(new EventosEstoque.OrdemAutomaticaCriada("o1", 1L, "x", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE)));
    }

    @Test
    void ligarExigeNumeroValidoEGravaAEvidenciaDoQueAconteceu() {
        loja.setAlertaProducaoAtivo(false);
        loja.setWhatsappDona(null);
        when(audit.userId()).thenReturn("u1");
        var u = new Usuario();
        u.setNome("Dona Rosa");
        when(usuarios.findById("u1")).thenReturn(Optional.of(u));
        assertThrows(BusinessException.class, () -> service.salvar("123", true));
        assertThrows(BusinessException.class, () -> service.salvar("", true));
        service.salvar("(11) 98765-4321", true);
        assertEquals("+5511987654321", loja.getWhatsappDona());
        assertTrue(loja.isAlertaProducaoAtivo());
        assertTrue(loja.getAlertaEvidencia().contains("Dona Rosa"));
        assertTrue(loja.getAlertaEvidencia().contains("+5511987654321"));
        assertNotNull(loja.getAlertaAtivadoEm());
        service.salvar("(11) 98765-4321", false);
        assertFalse(loja.isAlertaProducaoAtivo());
    }

    private static ProdutoImagemRepository.Resumo resumo(String produto, String id) {
        var r = mock(ProdutoImagemRepository.Resumo.class);
        when(r.getIdProduto()).thenReturn(produto);
        when(r.getId()).thenReturn(id);
        return r;
    }
}

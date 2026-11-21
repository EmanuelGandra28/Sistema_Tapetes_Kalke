package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.MensagemModelo;
import br.com.tapeteskalke.entity.MensagemModeloHistorico;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.MensagemModeloHistoricoRepository;
import br.com.tapeteskalke.repository.MensagemModeloRepository;
import br.com.tapeteskalke.repository.UsuarioRepository;
import java.util.*;
import org.junit.jupiter.api.Test;

class MensagemServiceTest {
    private final MensagemModeloRepository repo = mock(MensagemModeloRepository.class);
    private final MensagemModeloHistoricoRepository historicos = mock(MensagemModeloHistoricoRepository.class);
    private final MensagemService service = new MensagemService(repo, historicos, mock(UsuarioRepository.class), mock(AuditService.class));

    private static MensagemCatalogo.Definicao def(String chave) { return MensagemCatalogo.buscar(chave).orElseThrow(); }

    private MensagemModelo modelo(String chave, String texto, boolean ativo) {
        var m = new MensagemModelo();
        m.setId("m-" + chave);
        m.setChave(chave);
        m.setTexto(texto);
        m.setAtivo(ativo);
        when(repo.findByChave(chave)).thenReturn(Optional.of(m));
        when(repo.save(any(MensagemModelo.class))).thenAnswer(i -> i.getArgument(0));
        return m;
    }

    private static String codigo(Runnable r) { return assertThrows(BusinessException.class, r::run).code(); }

    @Test
    void todosOsTextosPadraoSaoValidos() {
        for (var d : MensagemCatalogo.todas()) assertDoesNotThrow(() -> MensagemService.validar(d, d.textoPadrao()), d.chave());
    }

    @Test
    void campoComErroDeDigitacaoImpedeSalvar() {
        var d = def("REATIVACAO_1");
        assertEquals("CAMPO_DESCONHECIDO", codigo(() -> MensagemService.validar(d, "Olá {nme}!")));
        assertEquals("CHAVES_SOLTAS", codigo(() -> MensagemService.validar(d, "Olá {nome e {loja}")));
        assertEquals("TEXTO_VAZIO", codigo(() -> MensagemService.validar(d, "   ")));
        assertEquals("TEXTO_GRANDE", codigo(() -> MensagemService.validar(d, "a".repeat(MensagemCatalogo.TAMANHO_MAXIMO + 1))));
    }

    @Test
    void camposObrigatoriosNaoPodemSerRemovidos() {
        assertEquals("CAMPO_OBRIGATORIO", codigo(() -> MensagemService.validar(def("ALERTA_PRODUCAO_DONA"), "Atenção, produzir tapetes. {link}")));
        assertEquals("CAMPO_OBRIGATORIO", codigo(() -> MensagemService.validar(def("ENVIO_ENCOMENDA"), "Sua encomenda foi enviada, {nome}.")));
    }

    @Test
    void linkEncurtadoEBloqueadoELinkEmCampanhaGeraAviso() {
        var campanha = def("REATIVACAO_2");
        assertEquals("LINK_ENCURTADO", codigo(() -> MensagemService.validar(campanha, "Veja em https://bit.ly/abc {nome}")));
        assertEquals("LINK_ENCURTADO", codigo(() -> MensagemService.validar(campanha, "Veja em www.tinyurl.com/x")));
        assertEquals(1, MensagemService.validar(campanha, "Veja {nome}: https://tapeteskalke.com.br/novidades").size());
        assertTrue(MensagemService.validar(def("ENVIO_ENCOMENDA"), "{nome}, rastreio {codigo_rastreio}: https://rastreio.com.br/x").isEmpty());
    }

    @Test
    void montarTrocaCamposELancaErroSeFaltarDado() {
        var d = def("BOTAO_WHATSAPP_CLIENTE");
        assertEquals("Oi Ana da Kalke", MensagemService.montar(d, "Oi {nome} da {loja}", Map.of("nome", "Ana", "loja", "Kalke")));
        assertEquals("DADO_FALTANTE", codigo(() -> MensagemService.montar(d, "Oi {nome} da {loja}", Map.of("nome", "Ana"))));
        // dado com "$" ou "\" não pode quebrar a troca
        assertEquals("Total R$ 10", MensagemService.montar(d, "Total {nome}", Map.of("nome", "R$ 10")));
    }

    @Test
    void campanhaSempreLevaAFraseDeSaidaEAvisoDePedidoNao() {
        var campanha = MensagemService.montar(def("REATIVACAO_2"), "Oi {nome}", Map.of("nome", "Ana"));
        assertTrue(campanha.endsWith(MensagemCatalogo.RODAPE_SAIDA));
        var pedido = MensagemService.montar(def("SAIU_PARA_ENTREGA"), "Oi {nome}", Map.of("nome", "Ana"));
        assertFalse(pedido.contains("SAIR"));
        // se o texto já tiver a frase, não duplica
        var jaTem = MensagemService.montar(def("REATIVACAO_2"), "Oi {nome}. " + MensagemCatalogo.RODAPE_SAIDA, Map.of("nome", "Ana"));
        assertEquals(1, jaTem.split("Responda SAIR", -1).length - 1);
    }

    @Test
    void salvarMudancaGuardaOTextoAnteriorNoHistorico() {
        var m = modelo("POS_VENDA", def("POS_VENDA").textoPadrao(), true);
        var r = service.salvar("POS_VENDA", "Oi {nome}, gostou do {produto}?", null);
        assertEquals("Oi {nome}, gostou do {produto}?", m.getTexto());
        assertTrue(r.modelo().personalizado());
        var captor = org.mockito.ArgumentCaptor.forClass(MensagemModeloHistorico.class);
        verify(historicos).save(captor.capture());
        assertEquals(def("POS_VENDA").textoPadrao(), captor.getValue().getTextoAnterior());
        assertEquals("POS_VENDA", captor.getValue().getChave());
    }

    @Test
    void salvarOMesmoTextoOuSoOInterruptorNaoPoluiOHistorico() {
        var m = modelo("POS_VENDA", def("POS_VENDA").textoPadrao(), true);
        service.salvar("POS_VENDA", null, false);
        assertFalse(m.isAtivo());
        service.salvar("POS_VENDA", def("POS_VENDA").textoPadrao(), null);
        verify(historicos, never()).save(any());
    }

    @Test
    void textoInvalidoNaoAlteraNada() {
        var m = modelo("POS_VENDA", def("POS_VENDA").textoPadrao(), true);
        assertThrows(BusinessException.class, () -> service.salvar("POS_VENDA", "Oi {nme}", null));
        assertEquals(def("POS_VENDA").textoPadrao(), m.getTexto());
        verify(repo, never()).save(any());
    }

    @Test
    void restaurarPadraoVoltaOTextoOriginalEGuardaOAtual() {
        var m = modelo("ANIVERSARIO", "Texto da loja {nome}", true);
        var r = service.restaurarPadrao("ANIVERSARIO");
        assertEquals(def("ANIVERSARIO").textoPadrao(), m.getTexto());
        assertFalse(r.personalizado());
        verify(historicos).save(any(MensagemModeloHistorico.class));
    }

    @Test
    void restaurarVersaoAntigaExigeQueAindaSejaValida() {
        var m = modelo("POS_VENDA", "Atual {nome}", true);
        var antigo = new MensagemModeloHistorico();
        antigo.setId("h1");
        antigo.setChave("POS_VENDA");
        antigo.setTextoAnterior("Antigo {produto} para {nome}");
        when(historicos.findById("h1")).thenReturn(Optional.of(antigo));
        service.restaurarVersao("POS_VENDA", "h1");
        assertEquals("Antigo {produto} para {nome}", m.getTexto());
        var invalido = new MensagemModeloHistorico();
        invalido.setId("h2");
        invalido.setChave("POS_VENDA");
        invalido.setTextoAnterior("Campo removido {campo_velho}");
        when(historicos.findById("h2")).thenReturn(Optional.of(invalido));
        assertThrows(BusinessException.class, () -> service.restaurarVersao("POS_VENDA", "h2"));
    }

    @Test
    void renderizarUsaOTextoDaLojaEDevolveVazioQuandoDesativado() {
        modelo("COMPROVANTE_VENDA", "Compra {numero_venda} de {nome}", true);
        assertEquals(Optional.of("Compra 7 de Ana"), service.renderizar("COMPROVANTE_VENDA", Map.of("numero_venda", "7", "nome", "Ana")));
        modelo("COMPROVANTE_VENDA", "Compra {numero_venda} de {nome}", false);
        assertEquals(Optional.empty(), service.renderizar("COMPROVANTE_VENDA", Map.of("numero_venda", "7", "nome", "Ana")));
    }

    @Test
    void gatilhoDesconhecidoDaErroDeNaoEncontrado() {
        assertThrows(BusinessException.class, () -> service.renderizar("NAO_EXISTE", Map.of()));
    }
}

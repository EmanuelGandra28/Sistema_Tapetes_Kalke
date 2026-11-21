package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.WhatsappEvento;
import br.com.tapeteskalke.repository.WhatsappEventoRepository;
import br.com.tapeteskalke.service.WebhookWhatsappService.Resultado;
import java.time.*;
import org.junit.jupiter.api.Test;

class WebhookWhatsappServiceTest {
    private static final String SEGREDO = "owh_teste";
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final long agora = clock.instant().getEpochSecond();
    private final WhatsappEventoRepository eventos = mock(WhatsappEventoRepository.class);
    private final ReativacaoService reativacao = mock(ReativacaoService.class);
    private final WebhookWhatsappService service = new WebhookWhatsappService(eventos, reativacao, clock, SEGREDO);

    private Resultado enviar(String corpo, long timestamp) {
        var ts = String.valueOf(timestamp);
        return service.processar(corpo, ts, WhatsappUtil.assinar(SEGREDO, ts, corpo));
    }

    @Test
    void assinaturaErradaOuAusenteERecusada() {
        var corpo = "{\"evento_id\":\"e1\",\"evento\":\"mensagem.situacao\"}";
        assertEquals(Resultado.ASSINATURA_INVALIDA, service.processar(corpo, String.valueOf(agora), "sha256=00"));
        assertEquals(Resultado.ASSINATURA_INVALIDA, service.processar(corpo, String.valueOf(agora), null));
        verifyNoInteractions(reativacao);
        verify(eventos, never()).save(any());
    }

    @Test
    void semSegredoConfiguradoRecusaTudo() {
        var s = new WebhookWhatsappService(eventos, reativacao, clock, "");
        var corpo = "{\"evento_id\":\"e1\",\"evento\":\"mensagem.situacao\"}";
        assertEquals(Resultado.ASSINATURA_INVALIDA, s.processar(corpo, String.valueOf(agora), WhatsappUtil.assinar("qualquer", String.valueOf(agora), corpo)));
    }

    @Test
    void timestampVelhoOuFuturoDemaisEhRecusado() {
        var corpo = "{\"evento_id\":\"e1\",\"evento\":\"mensagem.situacao\"}";
        assertEquals(Resultado.FORA_DA_JANELA, enviar(corpo, agora - 301));
        assertEquals(Resultado.FORA_DA_JANELA, enviar(corpo, agora + 301));
        assertEquals(Resultado.PROCESSADO, enviar(corpo, agora - 299));
    }

    @Test
    void eventoRepetidoEIgnorado() {
        var corpo = "{\"evento_id\":\"e1\",\"evento\":\"mensagem.situacao\",\"mensagem_id\":\"m1\",\"situacao\":\"entregue\"}";
        when(eventos.existsByEventoId("e1")).thenReturn(true);
        assertEquals(Resultado.DUPLICADO, enviar(corpo, agora));
        verifyNoInteractions(reativacao);
    }

    @Test
    void situacaoDaMensagemAtualizaOControle() {
        var corpo = "{\"evento_id\":\"e2\",\"evento\":\"mensagem.situacao\",\"mensagem_id\":\"m1\",\"situacao\":\"entregue\"}";
        assertEquals(Resultado.PROCESSADO, enviar(corpo, agora));
        verify(reativacao).atualizarSituacao("m1", "entregue");
        verify(eventos).save(any(WhatsappEvento.class));
    }

    @Test
    void respostaRecebidaEhLigadaAoClientePeloTelefone() {
        var corpo = "{\"evento_id\":\"e3\",\"evento\":\"mensagem.recebida\",\"remetente\":\"+5511987654321\",\"texto\":\"Quero ver as opções\",\"recebido_em\":\"2026-11-21T14:59:00Z\"}";
        assertEquals(Resultado.PROCESSADO, enviar(corpo, agora));
        verify(reativacao).registrarResposta(eq("+5511987654321"), eq("Quero ver as opções"), any());
    }

    @Test
    void descadastroPeloWhatsappRevogaAAutorizacao() {
        var corpo = "{\"evento_id\":\"e4\",\"evento\":\"contato.descadastrado\",\"destinatario\":\"+5511987654321\",\"motivo\":\"pedido_pelo_whatsapp\"}";
        assertEquals(Resultado.PROCESSADO, enviar(corpo, agora));
        verify(reativacao).registrarDescadastro("+5511987654321");
    }

    @Test
    void corpoQueNaoEJsonOuSemIdentificacaoEInvalido() {
        assertEquals(Resultado.CORPO_INVALIDO, enviar("isto não é json", agora));
        assertEquals(Resultado.CORPO_INVALIDO, enviar("{\"evento\":\"mensagem.situacao\"}", agora));
    }
}

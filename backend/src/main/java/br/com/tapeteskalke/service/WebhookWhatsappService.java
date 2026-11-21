package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.WhatsappEvento;
import br.com.tapeteskalke.repository.WhatsappEventoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Recebe os eventos do gateway (situação das mensagens, respostas dos clientes, pedidos de descadastro).
// Confere a assinatura com o corpo bruto, recusa timestamp velho, ignora evento repetido e nunca registra texto de mensagem em log.
@Service
public class WebhookWhatsappService {
    public enum Resultado { PROCESSADO, DUPLICADO, ASSINATURA_INVALIDA, FORA_DA_JANELA, CORPO_INVALIDO }

    private static final long TOLERANCIA_SEGUNDOS = 300;

    private final WhatsappEventoRepository eventos;
    private final ReativacaoService reativacao;
    private final Clock clock;
    private final String segredo;
    private final ObjectMapper json = new ObjectMapper();

    public WebhookWhatsappService(WhatsappEventoRepository eventos, ReativacaoService reativacao, Clock clock, @Value("${app.whatsapp.webhook-secret:}") String segredo) {
        this.eventos = eventos;
        this.reativacao = reativacao;
        this.clock = clock;
        this.segredo = segredo == null ? "" : segredo.trim();
    }

    // Na mesma transação do registro do evento: se algo falhar, nada fica gravado e o gateway repete a entrega depois.
    @Transactional
    public Resultado processar(String corpoBruto, String timestamp, String assinatura) {
        if (!WhatsappUtil.assinaturaValida(segredo, timestamp, corpoBruto, assinatura)) return Resultado.ASSINATURA_INVALIDA;
        try {
            if (Math.abs(clock.instant().getEpochSecond() - Long.parseLong(timestamp.trim())) > TOLERANCIA_SEGUNDOS) return Resultado.FORA_DA_JANELA;
        } catch (NumberFormatException e) {
            return Resultado.FORA_DA_JANELA;
        }
        JsonNode n;
        try {
            n = json.readTree(corpoBruto);
        } catch (Exception e) {
            return Resultado.CORPO_INVALIDO;
        }
        var eventoId = n.path("evento_id").asText("");
        var tipo = n.path("evento").asText("");
        if (eventoId.isBlank() || tipo.isBlank()) return Resultado.CORPO_INVALIDO;
        if (eventos.existsByEventoId(eventoId)) return Resultado.DUPLICADO;
        var e = new WhatsappEvento();
        e.setEventoId(eventoId);
        e.setEvento(tipo);
        e.setRecebidoEm(OffsetDateTime.now(clock));
        eventos.save(e);
        switch (tipo) {
            case "mensagem.situacao" -> reativacao.atualizarSituacao(n.path("mensagem_id").asText(""), n.path("situacao").asText(""));
            case "mensagem.recebida" -> {
                var texto = n.hasNonNull("texto") ? n.get("texto").asText() : n.path("legenda").asText(null);
                var remetente = WhatsappUtil.telefoneE164(n.path("remetente").asText(null));
                if (remetente != null) reativacao.registrarResposta(remetente, texto, parse(n.path("recebido_em").asText(null)));
            }
            case "contato.descadastrado" -> {
                var tel = WhatsappUtil.telefoneE164(n.path("destinatario").asText(null));
                if (tel != null) reativacao.registrarDescadastro(tel);
            }
            default -> { /* enquete.voto e outros: não usados */ }
        }
        return Resultado.PROCESSADO;
    }

    private OffsetDateTime parse(String texto) {
        try { return texto == null ? null : OffsetDateTime.parse(texto); } catch (Exception e) { return null; }
    }
}

package br.com.tapeteskalke.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// Cliente do Orquestra WhatsApp Gateway. A chave de API fica só no servidor (variável de ambiente), serve apenas para pedir o token
// de 5 minutos (guardado em memória e renovado antes de vencer) e nunca vai para log, tela ou banco. Nenhum texto de mensagem é logado.
@Component
public class GatewayWhatsAppClient implements WhatsAppProvider {
    private static final int TENTATIVAS = 3;

    private final RestClient http;
    private final String chave;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper();
    private String token;
    private Instant tokenExpira = Instant.EPOCH;
    long esperaBaseMs = 1000; // ajustável nos testes

    @Autowired
    public GatewayWhatsAppClient(RestClient.Builder builder, Clock clock, @Value("${app.whatsapp.url}") String url, @Value("${app.whatsapp.api-key:}") String chave) {
        this.http = builder.baseUrl(url).build();
        this.chave = chave == null ? "" : chave.trim();
        this.clock = clock;
    }

    @Override
    public boolean configurado() { return !chave.isBlank(); }

    private synchronized String token(boolean renovar) {
        if (!renovar && token != null && clock.instant().isBefore(tokenExpira)) return token;
        var r = chamar(() -> http.post().uri("/v1/autenticacao/token").header("X-API-Key", chave).retrieve().body(JsonNode.class));
        token = r.path("token_acesso").asText();
        // renova 30 s antes de vencer
        tokenExpira = clock.instant().plusSeconds(Math.max(30, r.path("expira_em_segundos").asLong(300) - 30));
        return token;
    }

    @Override
    public Enviada enviarTexto(String destinatario, String texto, String finalidade, String idempotencyKey, String evidencia) {
        if (!configurado()) throw new GatewayException(503, "GATEWAY_NAO_CONFIGURADO", "A chave do gateway de WhatsApp não está configurada no servidor", null);
        var corpo = new LinkedHashMap<String, Object>();
        corpo.put("destinatario", destinatario);
        corpo.put("texto", texto);
        corpo.put("finalidade", finalidade);
        if (evidencia != null && !evidencia.isBlank()) corpo.put("consentimento", java.util.Map.of("evidencia", evidencia));
        var r = comToken((t) -> enviar(corpo, idempotencyKey, t));
        return new Enviada(r.path("mensagem_id").asText(), r.path("situacao").asText("na_fila"));
    }

    @Override
    public Enviada enviarImagem(String destinatario, byte[] dados, String contentType, String legenda, String finalidade, String idempotencyKey, String evidencia) {
        if (!configurado()) throw new GatewayException(503, "GATEWAY_NAO_CONFIGURADO", "A chave do gateway de WhatsApp não está configurada no servidor", null);
        var arquivo = comToken((t) -> upload(dados, contentType, t)).path("arquivo_id").asText();
        var corpo = new LinkedHashMap<String, Object>();
        corpo.put("destinatario", destinatario);
        corpo.put("arquivo_id", arquivo);
        if (legenda != null && !legenda.isBlank()) corpo.put("legenda", legenda);
        corpo.put("finalidade", finalidade);
        if (evidencia != null && !evidencia.isBlank()) corpo.put("consentimento", java.util.Map.of("evidencia", evidencia));
        var r = comToken((t) -> enviar(corpo, idempotencyKey, t));
        return new Enviada(r.path("mensagem_id").asText(), r.path("situacao").asText("na_fila"));
    }

    // Token vencido ou revogado: renova e repete UMA vez, com a mesma chave de idempotência.
    private JsonNode comToken(java.util.function.Function<String, JsonNode> acao) {
        try {
            return chamar(() -> acao.apply(token(false)));
        } catch (GatewayException e) {
            if (e.status() != 401) throw e;
            return chamar(() -> acao.apply(token(true)));
        }
    }

    private JsonNode upload(byte[] dados, String contentType, String bearer) {
        // Montagem manual (o MultipartBodyBuilder do Spring exige uma biblioteca reativa que o projeto não usa).
        var cabecalho = new org.springframework.http.HttpHeaders();
        cabecalho.setContentType(MediaType.parseMediaType(contentType));
        var parte = new org.springframework.http.HttpEntity<>(new org.springframework.core.io.ByteArrayResource(dados) {
            @Override public String getFilename() { return "foto"; }
        }, cabecalho);
        var corpo = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        corpo.add("arquivo", parte);
        return http.post().uri("/v1/arquivos").header("Authorization", "Bearer " + bearer).contentType(MediaType.MULTIPART_FORM_DATA).body(corpo).retrieve().body(JsonNode.class);
    }

    private JsonNode enviar(Object corpo, String idempotencyKey, String bearer) {
        return http.post().uri("/v1/mensagens").header("Authorization", "Bearer " + bearer).header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().body(JsonNode.class);
    }

    // Até 3 tentativas com espera crescente, só para erro de rede, 429 e 5xx. 400/402/403/409/422 nunca se repetem.
    private JsonNode chamar(java.util.function.Supplier<JsonNode> acao) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return acao.get();
            } catch (RestClientResponseException e) {
                var erro = traduzir(e);
                boolean repetir = (e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError()) && tentativa < TENTATIVAS;
                if (!repetir) throw erro;
            } catch (ResourceAccessException e) {
                if (tentativa >= TENTATIVAS) throw new GatewayException(503, "GATEWAY_INDISPONIVEL", "Não foi possível falar com o gateway de WhatsApp", null);
            }
            dormir(esperaBaseMs * (1L << (tentativa - 1)));
        }
    }

    private static void dormir(long ms) {
        try { if (ms > 0) Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private GatewayException traduzir(RestClientResponseException e) {
        String codigo = null, titulo = null, requisicao = null;
        try {
            var n = json.readTree(e.getResponseBodyAsString());
            codigo = n.path("codigo").asText(null);
            titulo = n.path("title").asText(null);
            requisicao = n.path("requisicao_id").asText(null);
        } catch (Exception ignorado) { /* corpo que não é JSON: usa só o status */ }
        return new GatewayException(e.getStatusCode().value(), codigo == null ? "ERRO_" + e.getStatusCode().value() : codigo,
                titulo == null ? "O gateway de WhatsApp respondeu com erro " + e.getStatusCode().value() : titulo, requisicao);
    }
}

package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GatewayWhatsAppClientTest {
    private static final String TOKEN = "{\"token_acesso\":\"tok-1\",\"token_tipo\":\"Bearer\",\"expira_em_segundos\":300,\"tipo_chave\":\"individual\",\"escopos\":[]}";
    private static final String ACEITA = "{\"mensagem_id\":\"m-1\",\"situacao\":\"na_fila\"}";

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(false).build();
    private final GatewayWhatsAppClient client = new GatewayWhatsAppClient(builder, clock, "http://gw", "CHAVE-SECRETA");

    private void token(String corpo) {
        server.expect(requestTo("http://gw/v1/autenticacao/token")).andExpect(method(HttpMethod.POST)).andExpect(header("X-API-Key", "CHAVE-SECRETA"))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
    }

    private void mensagem(org.springframework.test.web.client.ResponseCreator resposta) {
        server.expect(requestTo("http://gw/v1/mensagens")).andExpect(method(HttpMethod.POST)).andExpect(header("Idempotency-Key", "chave-1")).andRespond(resposta);
    }

    @Test
    void enviaComTokenIdempotenciaEEvidenciaDoConsentimento() {
        token(TOKEN);
        server.expect(requestTo("http://gw/v1/mensagens")).andExpect(header("Authorization", "Bearer tok-1")).andExpect(header("Idempotency-Key", "chave-1"))
                .andExpect(jsonPath("$.destinatario").value("+5511987654321")).andExpect(jsonPath("$.finalidade").value("marketing_reativacao"))
                .andExpect(jsonPath("$.consentimento.evidencia").value("Autorizou no balcão")).andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED).body(ACEITA).contentType(MediaType.APPLICATION_JSON));
        var r = client.enviarTexto("+5511987654321", "Olá", "marketing_reativacao", "chave-1", "Autorizou no balcão");
        assertEquals("m-1", r.mensagemId());
        server.verify();
    }

    @Test
    void tokenFicaEmCacheEntreEnvios() {
        token(TOKEN); // uma única chamada de token para dois envios
        mensagem(withSuccess(ACEITA, MediaType.APPLICATION_JSON));
        mensagem(withSuccess(ACEITA, MediaType.APPLICATION_JSON));
        client.enviarTexto("+5511987654321", "a", "f", "chave-1", null);
        client.enviarTexto("+5511987654321", "b", "f", "chave-1", null);
        server.verify();
    }

    @Test
    void token401RenovaEUmaVezRepeteComAMesmaChave() {
        token(TOKEN);
        mensagem(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED).body("{\"codigo\":\"TOKEN_INVALIDO\",\"title\":\"Token inválido\"}").contentType(MediaType.APPLICATION_PROBLEM_JSON));
        token(TOKEN.replace("tok-1", "tok-2"));
        server.expect(requestTo("http://gw/v1/mensagens")).andExpect(header("Authorization", "Bearer tok-2")).andExpect(header("Idempotency-Key", "chave-1"))
                .andRespond(withSuccess(ACEITA, MediaType.APPLICATION_JSON));
        assertEquals("m-1", client.enviarTexto("+5511987654321", "a", "f", "chave-1", null).mensagemId());
        server.verify();
    }

    @Test
    void erroDeConsentimentoViraExcecaoComCodigoETituloSemRepetir() {
        token(TOKEN);
        mensagem(withStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY).body("{\"codigo\":\"CONSENTIMENTO_AUSENTE\",\"title\":\"Sem consentimento\",\"requisicao_id\":\"r-9\"}").contentType(MediaType.APPLICATION_PROBLEM_JSON));
        var e = assertThrows(GatewayException.class, () -> client.enviarTexto("+5511987654321", "a", "f", "chave-1", null));
        assertEquals(422, e.status());
        assertEquals("CONSENTIMENTO_AUSENTE", e.codigo());
        assertEquals("r-9", e.requisicaoId());
        assertTrue(e.definitivo());
        assertFalse(e.deveParar());
        server.verify(); // só uma tentativa de envio
    }

    @Test
    void semSaldoOuSemPermissaoManda_parar() {
        token(TOKEN);
        mensagem(withStatus(org.springframework.http.HttpStatus.PAYMENT_REQUIRED).body("{\"codigo\":\"LIMITE_DO_PLANO_ATINGIDO\",\"title\":\"Limite atingido\"}").contentType(MediaType.APPLICATION_PROBLEM_JSON));
        assertTrue(assertThrows(GatewayException.class, () -> client.enviarTexto("+5511987654321", "a", "f", "chave-1", null)).deveParar());
    }

    @Test
    void erro503EhRepetidoEDepoisFunciona() {
        client.esperaBaseMs = 0;
        token(TOKEN);
        mensagem(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        mensagem(withSuccess(ACEITA, MediaType.APPLICATION_JSON));
        assertEquals("m-1", client.enviarTexto("+5511987654321", "a", "f", "chave-1", null).mensagemId());
        server.verify();
    }

    @Test
    void semChaveNaoEnviaNada() {
        var sem = new GatewayWhatsAppClient(RestClient.builder(), clock, "http://gw", "  ");
        assertFalse(sem.configurado());
        assertEquals(503, assertThrows(GatewayException.class, () -> sem.enviarTexto("+5511987654321", "a", "f", "k", null)).status());
    }

    @Test
    void enviaFotoEmDuasEtapasUploadEDepoisMensagemComLegenda() {
        token(TOKEN);
        server.expect(requestTo("http://gw/v1/arquivos")).andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer tok-1"))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.startsWith("multipart/form-data")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED).body("{\"arquivo_id\":\"f-1\",\"situacao\":\"disponivel\"}").contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://gw/v1/mensagens")).andExpect(header("Idempotency-Key", "chave-1")).andExpect(jsonPath("$.arquivo_id").value("f-1"))
                .andExpect(jsonPath("$.legenda").value("Tapete Floral")).andExpect(jsonPath("$.destinatario").value("+5511987654321"))
                .andRespond(withSuccess(ACEITA, MediaType.APPLICATION_JSON));
        var r = client.enviarImagem("+5511987654321", new byte[]{1, 2, 3}, "image/webp", "Tapete Floral", "aviso_operacional", "chave-1", "Dona ativou o alerta");
        assertEquals("m-1", r.mensagemId());
        server.verify();
    }
}

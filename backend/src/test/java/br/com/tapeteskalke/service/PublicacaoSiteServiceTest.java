package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.tapeteskalke.exception.BusinessException;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PublicacaoSiteServiceTest {
    static class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-11-21T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    /** Cliente falso do GitHub: guarda o que recebeu e devolve o status combinado (ou falha de rede). */
    static class GithubFalso implements PublicacaoSiteService.GithubClient {
        final List<String> chamadas = new ArrayList<>();
        int status = 204;
        boolean cairRede = false;
        @Override public int dispararWorkflow(String repo, String workflow, String token) throws IOException {
            chamadas.add(repo + "|" + workflow + "|" + token);
            if (cairRede) throw new IOException("sem rede");
            return status;
        }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final GithubFalso github = new GithubFalso();

    private PublicacaoSiteService servico(String token) {
        return new PublicacaoSiteService(github, relogio, token, "dono/site", "publicar.yml");
    }

    @Test
    void semTokenDizQueAindaNaoFoiConfigurado() {
        var e = assertThrows(BusinessException.class, () -> servico("").publicar());
        assertEquals("SITE_NAO_CONFIGURADO", e.code());
        assertEquals(HttpStatus.BAD_REQUEST, e.status());
        assertTrue(github.chamadas.isEmpty());
    }

    @Test
    void dispararComSucessoUsaRepositorioWorkflowETokenConfigurados() {
        servico("segredo").publicar();
        assertEquals(List.of("dono/site|publicar.yml|segredo"), github.chamadas);
    }

    @Test
    void erroDoGithubViraMensagemAmigavel() {
        github.status = 404;
        var e = assertThrows(BusinessException.class, () -> servico("t").publicar());
        assertEquals("SITE_FALHOU", e.code());
        assertEquals(HttpStatus.BAD_GATEWAY, e.status());
        assertFalse(e.getMessage().contains("404"));
    }

    @Test
    void faltaDeRedeTambemViraMensagemAmigavel() {
        github.cairRede = true;
        assertEquals("SITE_FALHOU", assertThrows(BusinessException.class, () -> servico("t").publicar()).code());
    }

    @Test
    void segundoPedidoEmMenosDeDoisMinutosEhRecusado() {
        var s = servico("t");
        s.publicar();
        relogio.agora = relogio.agora.plusSeconds(60);
        var e = assertThrows(BusinessException.class, s::publicar);
        assertEquals("SITE_AGUARDE", e.code());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.status());
        assertEquals(1, github.chamadas.size());
        relogio.agora = relogio.agora.plusSeconds(61);
        s.publicar();
        assertEquals(2, github.chamadas.size());
    }

    @Test
    void pedidoQueFalhouNaoObrigaAEsperar() {
        var s = servico("t");
        github.status = 500;
        assertThrows(BusinessException.class, s::publicar);
        github.status = 204;
        assertDoesNotThrow(s::publicar);
    }
}

package br.com.tapeteskalke.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class PortfolioControllerTest {
    private static HttpServletRequest requisicao(String encaminhado) {
        var r = mock(HttpServletRequest.class);
        when(r.getHeader("X-Forwarded-For")).thenReturn(encaminhado);
        when(r.getRemoteAddr()).thenReturn("10.0.0.9");
        return r;
    }

    @Test
    void usaOUltimoIpDoCabecalhoQueOProxyAcrescentou() {
        assertEquals("2.2.2.2", PortfolioController.ipDe(requisicao("9.9.9.9, 2.2.2.2")));
    }

    @Test
    void ignoraValoresEmBrancoNoFimDoCabecalho() {
        assertEquals("1.1.1.1", PortfolioController.ipDe(requisicao("1.1.1.1, ")));
    }

    @Test
    void semCabecalhoOuComCabecalhoVazioUsaOEnderecoDaConexao() {
        assertEquals("10.0.0.9", PortfolioController.ipDe(requisicao(null)));
        assertEquals("10.0.0.9", PortfolioController.ipDe(requisicao("  ,  ")));
    }
}

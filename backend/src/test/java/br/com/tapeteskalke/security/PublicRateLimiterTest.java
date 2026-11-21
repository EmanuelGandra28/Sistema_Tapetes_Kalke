package br.com.tapeteskalke.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.tapeteskalke.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PublicRateLimiterTest {
    static class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-11-21T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final PublicRateLimiter limiter = new PublicRateLimiter(relogio);

    @Test
    void permiteAte30PorMinutoEBloqueiaA31() {
        for (int i = 0; i < PublicRateLimiter.MAXIMO_POR_MINUTO; i++) limiter.permitir("1.1.1.1");
        var e = assertThrows(BusinessException.class, () -> limiter.permitir("1.1.1.1"));
        assertEquals("MUITAS_REQUISICOES", e.code());
    }

    @Test
    void outroIpNaoEAfetado() {
        for (int i = 0; i < PublicRateLimiter.MAXIMO_POR_MINUTO; i++) limiter.permitir("1.1.1.1");
        assertDoesNotThrow(() -> limiter.permitir("2.2.2.2"));
    }

    @Test
    void liberaDepoisDeUmMinuto() {
        for (int i = 0; i < PublicRateLimiter.MAXIMO_POR_MINUTO; i++) limiter.permitir("1.1.1.1");
        relogio.agora = relogio.agora.plusSeconds(61);
        assertDoesNotThrow(() -> limiter.permitir("1.1.1.1"));
    }
}

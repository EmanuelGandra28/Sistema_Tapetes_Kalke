package br.com.tapeteskalke.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.tapeteskalke.exception.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LoginAttemptServiceTest {
    /** Relógio controlável para simular a passagem da janela de bloqueio. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-27T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service = new LoginAttemptService(clock);

    private void falhar(int vezes, String ip) {
        for (int i = 0; i < vezes; i++) service.failed("dono@kalke.com", ip);
    }

    @Test
    void bloqueiaAposCincoFalhas() {
        falhar(4, "1.1.1.1");
        assertDoesNotThrow(() -> service.checkAllowed("dono@kalke.com", "1.1.1.1"));
        falhar(1, "1.1.1.1");
        var e = assertThrows(BusinessException.class, () -> service.checkAllowed("DONO@kalke.com ", "1.1.1.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.status());
    }

    @Test
    void bloqueioNaoAfetaOutroIp() {
        falhar(5, "1.1.1.1");
        assertDoesNotThrow(() -> service.checkAllowed("dono@kalke.com", "2.2.2.2"));
    }

    @Test
    void sucessoZeraContagem() {
        falhar(4, "1.1.1.1");
        service.succeeded("dono@kalke.com", "1.1.1.1");
        falhar(4, "1.1.1.1");
        assertDoesNotThrow(() -> service.checkAllowed("dono@kalke.com", "1.1.1.1"));
    }

    @Test
    void liberaDepoisDaJanela() {
        falhar(5, "1.1.1.1");
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        assertDoesNotThrow(() -> service.checkAllowed("dono@kalke.com", "1.1.1.1"));
    }
}

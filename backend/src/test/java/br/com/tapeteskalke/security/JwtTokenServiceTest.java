package br.com.tapeteskalke.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtTokenServiceTest {
    private static final String SECRET = "k7Q2vX9pL4mN8rT1wZ6yB3cF5hJ0sD2gA9eU7iO4qW1xV8nM";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"curto", "change-this-development-secret-to-a-32-byte-minimum-value", "replace-with-a-random-base64-secret-at-least-32-bytes"})
    void recusaSegredoAusenteCurtoOuDeExemplo(String secret) {
        assertThrows(IllegalStateException.class, () -> new JwtTokenService(secret, 3600));
    }

    @Test
    void tokenEmitidoCarregaUsuarioCargoEVersaoDeSessao() {
        var service = new JwtTokenService(SECRET, 3600);
        var claims = service.parse(service.issue("u1", "a@b.com", "DONO", 7));
        assertEquals("u1", claims.getSubject());
        assertEquals("DONO", claims.get("role", String.class));
        assertEquals(7L, claims.get(JwtTokenService.SESSION_CLAIM, Number.class).longValue());
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRecusado() {
        var outro = new JwtTokenService("Z9y8X7w6V5u4T3s2R1q0P9o8N7m6L5k4J3i2", 3600);
        var token = outro.issue("u1", "a@b.com", "DONO", 0);
        assertThrows(Exception.class, () -> new JwtTokenService(SECRET, 3600).parse(token));
    }
}

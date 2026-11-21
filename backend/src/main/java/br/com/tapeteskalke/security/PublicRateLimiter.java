package br.com.tapeteskalke.security;

import br.com.tapeteskalke.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// Limita as rotas públicas (sem login) por IP: no máximo 30 requisições por minuto. Em memória, reinicia com a API.
@Service
public class PublicRateLimiter {
    public static final int MAXIMO_POR_MINUTO = 30;
    private static final Duration JANELA = Duration.ofMinutes(1);
    private static final int LIMPEZA_ACIMA_DE = 1000;

    private record Janela(Instant inicio, int total) {}

    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();
    private final Clock clock;

    public PublicRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void permitir(String ip) {
        var agora = Instant.now(clock);
        if (janelas.size() > LIMPEZA_ACIMA_DE) janelas.values().removeIf(j -> agora.isAfter(j.inicio().plus(JANELA)));
        var janela = janelas.compute(ip, (k, atual) ->
                atual == null || agora.isAfter(atual.inicio().plus(JANELA)) ? new Janela(agora, 1) : new Janela(atual.inicio(), atual.total() + 1));
        if (janela.total() > MAXIMO_POR_MINUTO)
            throw new BusinessException("MUITAS_REQUISICOES", "Muitas requisições. Tente novamente em instantes", HttpStatus.TOO_MANY_REQUESTS);
    }
}

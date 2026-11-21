package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.security.PublicRateLimiter;
import br.com.tapeteskalke.service.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioController {
    private final PortfolioService service;
    private final PublicRateLimiter limiter;

    public PortfolioController(PortfolioService service, PublicRateLimiter limiter) {
        this.service = service;
        this.limiter = limiter;
    }

    // Leitura pública (sem login) usada pelo site de portfólio na hora de gerar as páginas.
    @GetMapping("/api/v1/publico/catalogo")
    public ResponseEntity<ApiResponse<?>> catalogo(HttpServletRequest request) {
        limiter.permitir(ipDe(request));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()).body(ApiResponse.ok(service.catalogo()));
    }

    // Atrás do proxy do Render, o último valor de X-Forwarded-For é o que o próprio proxy acrescentou
    // (os anteriores podem ser forjados pelo visitante).
    static String ipDe(HttpServletRequest request) {
        var encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            var partes = encaminhado.split(",");
            return partes[partes.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}

package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.security.PublicRateLimiter;
import br.com.tapeteskalke.service.PermissionService;
import br.com.tapeteskalke.service.PortfolioService;
import br.com.tapeteskalke.service.PublicacaoSiteService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioController {
    private final PortfolioService service;
    private final PublicRateLimiter limiter;
    private final PublicacaoSiteService publicacao;
    private final PermissionService permissions;

    public PortfolioController(PortfolioService service, PublicRateLimiter limiter, PublicacaoSiteService publicacao, PermissionService permissions) {
        this.service = service;
        this.limiter = limiter;
        this.publicacao = publicacao;
        this.permissions = permissions;
    }

    // Leitura pública (sem login) usada pelo site de portfólio na hora de gerar as páginas.
    @GetMapping("/api/v1/publico/catalogo")
    public ResponseEntity<ApiResponse<?>> catalogo(HttpServletRequest request) {
        limiter.permitir(ipDe(request));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()).body(ApiResponse.ok(service.catalogo()));
    }

    // Botão "Atualizar site" (exige login e a permissão portfolio_publicar).
    @PostMapping("/api/v1/portfolio/publicar")
    public ApiResponse<?> publicar() {
        permissions.action("portfolio_publicar");
        publicacao.publicar();
        return ApiResponse.ok(null, "Atualização do site pedida. Ela leva de 2 a 5 minutos.");
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

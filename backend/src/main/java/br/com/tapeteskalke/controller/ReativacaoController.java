package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.service.PermissionService;
import br.com.tapeteskalke.service.ReativacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reativacao")
public class ReativacaoController {
    public record CriarRequest(@NotBlank String nome, @NotNull List<String> clienteIds, Integer intervaloDias, Integer limiteDiario) {}

    private final ReativacaoService service;
    private final PermissionService permissions;
    private final String cronSecret;

    public ReativacaoController(ReativacaoService service, PermissionService permissions, @Value("${app.whatsapp.cron-secret:}") String cronSecret) {
        this.service = service;
        this.permissions = permissions;
        this.cronSecret = cronSecret == null ? "" : cronSecret.trim();
    }

    @GetMapping("/status")
    public ApiResponse<?> status() {
        permissions.page("reativacao");
        return ApiResponse.ok(service.status());
    }

    @GetMapping("/candidatos")
    public ApiResponse<?> candidatos() {
        permissions.page("reativacao");
        return ApiResponse.ok(service.candidatos());
    }

    @GetMapping("/campanhas")
    public ApiResponse<?> campanhas() {
        permissions.page("reativacao");
        return ApiResponse.ok(service.listar());
    }

    @PostMapping("/campanhas")
    public ResponseEntity<ApiResponse<?>> criar(@Valid @RequestBody CriarRequest r) {
        permissions.action("reativacao_gerenciar");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.criar(r.nome(), r.clienteIds(), r.intervaloDias() == null ? 5 : r.intervaloDias(), r.limiteDiario() == null ? 20 : r.limiteDiario())));
    }

    @PostMapping("/campanhas/{id}/aprovar")
    public ApiResponse<?> aprovar(@PathVariable String id) {
        permissions.action("reativacao_gerenciar");
        return ApiResponse.ok(service.aprovar(id));
    }

    @PostMapping("/campanhas/{id}/pausar")
    public ApiResponse<?> pausar(@PathVariable String id) {
        permissions.action("reativacao_gerenciar");
        return ApiResponse.ok(service.pausar(id));
    }

    @PostMapping("/campanhas/{id}/cancelar")
    public ApiResponse<?> cancelar(@PathVariable String id) {
        permissions.action("reativacao_gerenciar");
        return ApiResponse.ok(service.cancelar(id));
    }

    @GetMapping("/campanhas/{id}/resultados")
    public ApiResponse<?> resultados(@PathVariable String id) {
        permissions.page("reativacao");
        return ApiResponse.ok(service.resultados(id));
    }

    // Chamado por uma rotina externa (GitHub Actions) a cada poucos minutos, sem login: protegido por um segredo no cabeçalho.
    @PostMapping("/processar")
    public ResponseEntity<ApiResponse<?>> processar(@RequestHeader(value = "X-Cron-Secret", required = false) String segredo) {
        if (cronSecret.isEmpty() || segredo == null || !MessageDigest.isEqual(cronSecret.getBytes(StandardCharsets.UTF_8), segredo.getBytes(StandardCharsets.UTF_8)))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(ApiResponse.ok(service.processarFila()));
    }
}

package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.service.AlertaProducaoService;
import br.com.tapeteskalke.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

// Alerta de produção para a dona: quem edita as configurações define o número e liga/desliga; também pode reenviar o que falhou.
@RestController
@RequestMapping("/api/v1/alertas-producao")
public class AlertaProducaoController {
    public record SalvarRequest(String whatsapp, boolean ativo) {}

    private final AlertaProducaoService service;
    private final PermissionService permissions;

    public AlertaProducaoController(AlertaProducaoService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    @GetMapping
    public ApiResponse<?> config() {
        permissions.action("configuracoes_editar");
        return ApiResponse.ok(service.config());
    }

    @PutMapping
    public ApiResponse<?> salvar(@Valid @RequestBody SalvarRequest r) {
        permissions.action("configuracoes_editar");
        return ApiResponse.ok(service.salvar(r.whatsapp(), r.ativo()));
    }

    // Recoloca os alertas que falharam na fila e tenta enviar agora (respeitando o horário comercial).
    @PostMapping("/reenviar")
    public ApiResponse<?> reenviar() {
        permissions.action("configuracoes_editar");
        service.reenviarFalhas();
        return ApiResponse.ok(service.processar());
    }
}

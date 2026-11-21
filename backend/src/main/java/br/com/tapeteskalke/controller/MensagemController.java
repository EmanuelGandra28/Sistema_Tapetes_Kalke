package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.service.MensagemService;
import br.com.tapeteskalke.service.PermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

// Aba "Mensagens" das Configurações: tudo exige a ação mensagens_gerenciar (Administrador sempre tem; Dono e Gerente por padrão).
@RestController
@RequestMapping("/api/v1/mensagens")
public class MensagemController {
    public record SalvarRequest(@NotNull String texto, Boolean ativo) {}
    public record AtivoRequest(@NotNull Boolean ativo) {}

    private final MensagemService service;
    private final PermissionService permissions;

    public MensagemController(MensagemService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    public record BotaoResponse(String texto) {}

    // Quem trabalha com clientes precisa ler o texto do botão de WhatsApp, mesmo sem poder editar mensagens.
    @GetMapping("/botao-whatsapp")
    public ApiResponse<BotaoResponse> botaoWhatsapp() {
        permissions.page("clientes");
        return ApiResponse.ok(new BotaoResponse(service.textoBotaoCliente().orElse(null)));
    }

    @GetMapping
    public ApiResponse<?> listar() {
        permissions.action("mensagens_gerenciar");
        return ApiResponse.ok(service.listar());
    }

    @PutMapping("/{chave}")
    public ApiResponse<?> salvar(@PathVariable String chave, @Valid @RequestBody SalvarRequest r) {
        permissions.action("mensagens_gerenciar");
        return ApiResponse.ok(service.salvar(chave, r.texto(), r.ativo()));
    }

    @PutMapping("/{chave}/ativo")
    public ApiResponse<?> ativar(@PathVariable String chave, @Valid @RequestBody AtivoRequest r) {
        permissions.action("mensagens_gerenciar");
        return ApiResponse.ok(service.salvar(chave, null, r.ativo()));
    }

    @PostMapping("/{chave}/restaurar-padrao")
    public ApiResponse<?> restaurarPadrao(@PathVariable String chave) {
        permissions.action("mensagens_gerenciar");
        return ApiResponse.ok(service.restaurarPadrao(chave));
    }

    @GetMapping("/{chave}/historico")
    public ApiResponse<?> historico(@PathVariable String chave) {
        permissions.action("mensagens_gerenciar");
        return ApiResponse.ok(service.historico(chave));
    }

    @PostMapping("/{chave}/historico/{id}/restaurar")
    public ApiResponse<?> restaurarVersao(@PathVariable String chave, @PathVariable String id) {
        permissions.action("mensagens_gerenciar");
        return ApiResponse.ok(service.restaurarVersao(chave, id));
    }
}

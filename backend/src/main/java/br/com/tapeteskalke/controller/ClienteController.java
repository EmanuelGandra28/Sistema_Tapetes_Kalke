package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.*;
import br.com.tapeteskalke.dto.ClienteDtos.*;
import br.com.tapeteskalke.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService service;
    private final PermissionService permissions;

    public ClienteController(ClienteService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    @GetMapping
    public ApiResponse<PageResponse<Response>> list(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        permissions.page("clientes");
        return ApiResponse.ok(service.list(search, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Response> get(@PathVariable String id) {
        permissions.page("clientes");
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody Request r) {
        permissions.action("clientes_criar");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Response> update(@PathVariable String id, @Valid @RequestBody Request r) {
        permissions.action("clientes_editar");
        return ApiResponse.ok(service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        permissions.action("clientes_excluir");
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historico-compras")
    public ApiResponse<?> history(@PathVariable String id) {
        permissions.page("clientes");
        service.get(id);
        return ApiResponse.ok(java.util.Map.of("message", "Histórico disponível pela listagem de vendas do cliente."));
    }

    @PostMapping("/{id}/whatsapp-link")
    public ApiResponse<?> whatsapp(@PathVariable String id) {
        permissions.page("clientes");
        var c = service.get(id);
        return ApiResponse.ok(java.util.Map.of("url", "https://wa.me/" + c.telefone() + "?text=" + java.net.URLEncoder.encode("Olá " + c.nome() + "! Esperamos que esteja bem! Temos novidades que podem te interessar!", java.nio.charset.StandardCharsets.UTF_8)));
    }
}
package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.service.ConsentimentoService;
import br.com.tapeteskalke.service.PermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Autorização do cliente para receber mensagens de campanha pelo WhatsApp (LGPD).
@RestController
@RequestMapping("/api/v1/clientes")
public class ConsentimentoController {
    public record RegistrarRequest(@NotBlank String evidencia) {}

    private final ConsentimentoService service;
    private final PermissionService permissions;

    public ConsentimentoController(ConsentimentoService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    @GetMapping("/consentimentos")
    public ApiResponse<?> idsComConsentimento() {
        permissions.page("clientes");
        return ApiResponse.ok(service.idsComConsentimento());
    }

    @PostMapping("/{id}/consentimento")
    public ResponseEntity<ApiResponse<?>> registrar(@PathVariable String id, @Valid @RequestBody RegistrarRequest r) {
        permissions.action("clientes_consentimento");
        service.registrar(id, r.evidencia());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @DeleteMapping("/{id}/consentimento")
    public ApiResponse<?> revogar(@PathVariable String id) {
        permissions.action("clientes_consentimento");
        service.revogar(id, "Revogado pela equipe na tela de clientes");
        return ApiResponse.ok(null);
    }
}

package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.dto.OperacaoDtos.DespesaRequest;
import br.com.tapeteskalke.service.DespesaService;
import br.com.tapeteskalke.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/despesas")
public class DespesaController {
    private final DespesaService service;
    private final PermissionService permissions;

    public DespesaController(DespesaService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    @GetMapping
    public ApiResponse<?> list() {
        permissions.page("despesas");
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody DespesaRequest r) {
        permissions.action("despesas_criar");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody DespesaRequest r) {
        permissions.action("despesas_editar");
        return ApiResponse.ok(service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id, @RequestParam String motivo) {
        permissions.action("despesas_excluir");
        service.delete(id, motivo);
        return ApiResponse.ok(null);
    }
}

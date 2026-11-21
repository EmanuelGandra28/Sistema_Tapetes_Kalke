package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.service.VistoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

// Sem permissão específica: qualquer usuário logado cuida do que ele mesmo já viu (o serviço só mexe nas linhas dele).
@RestController
@RequestMapping("/api/v1/vistos")
public class VistoController {
    public record MarcarRequest(@NotBlank String chave) {}

    private final VistoService service;

    public VistoController(VistoService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> listar() {
        return ApiResponse.ok(service.listar());
    }

    public record ContaResponse(java.time.OffsetDateTime criadaEm) {}

    @GetMapping("/conta")
    public ApiResponse<ContaResponse> conta() {
        return ApiResponse.ok(new ContaResponse(service.contaCriadaEm()));
    }

    @PostMapping
    public ApiResponse<?> marcar(@Valid @RequestBody MarcarRequest r) {
        service.marcar(r.chave());
        return ApiResponse.ok(null);
    }

    @DeleteMapping
    public ApiResponse<?> limpar(@RequestParam String prefixo) {
        service.limpar(prefixo);
        return ApiResponse.ok(null);
    }
}

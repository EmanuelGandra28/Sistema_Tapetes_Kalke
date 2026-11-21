package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.service.PermissionService;
import br.com.tapeteskalke.service.ProdutoImagemService;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProdutoImagemController {
    private final ProdutoImagemService service;
    private final PermissionService permissions;

    public ProdutoImagemController(ProdutoImagemService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    @GetMapping("/api/v1/produtos/{id}/imagens")
    public ApiResponse<?> listar(@PathVariable String id) {
        permissions.page("produtos");
        return ApiResponse.ok(service.listar(id));
    }

    // Miniaturas das listas (produtos, ordens de produção): mapa produto → foto principal.
    @GetMapping("/api/v1/produtos/imagens-principais")
    public ApiResponse<?> principais() {
        return ApiResponse.ok(service.principais());
    }

    @PostMapping(value = "/api/v1/produtos/{id}/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> enviar(@PathVariable String id, @RequestParam("arquivo") MultipartFile arquivo) {
        permissions.action("produtos_imagens");
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.adicionar(id, arquivo.getBytes())));
        } catch (IOException e) {
            throw new BusinessException("IMAGEM_ILEGIVEL", "Não foi possível ler a foto enviada", HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/api/v1/produtos/{id}/imagens/{imagemId}/principal")
    public ApiResponse<?> principal(@PathVariable String id, @PathVariable String imagemId) {
        permissions.action("produtos_imagens");
        service.definirPrincipal(id, imagemId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/v1/produtos/{id}/imagens/{imagemId}")
    public ApiResponse<?> remover(@PathVariable String id, @PathVariable String imagemId) {
        permissions.action("produtos_imagens");
        service.remover(id, imagemId);
        return ApiResponse.ok(null);
    }

    // Leitura pública (sem login): o endereço tem id aleatório e só serve fotos de produto.
    @GetMapping("/api/v1/imagens/{imagemId}")
    public ResponseEntity<byte[]> ver(@PathVariable String imagemId) {
        var i = service.conteudo(imagemId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(i.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff").body(i.getDados());
    }
}

package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.*;
import br.com.tapeteskalke.dto.OperacaoDtos.*;
import br.com.tapeteskalke.dto.ProdutoDtos.AjusteRequest;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.*;
import br.com.tapeteskalke.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estoque")
public class EstoqueController {
    private final ProdutoRepository produtos;
    private final ProdutoService productService;
    private final EstoqueService estoque;
    private final MovimentoService movimentos;
    private final InventarioService inventario;
    private final SugestaoCompraService sugestao;
    private final PermissionService permissions;

    public EstoqueController(ProdutoRepository produtos, ProdutoService productService, EstoqueService estoque, MovimentoService movimentos,
                             InventarioService inventario, SugestaoCompraService sugestao, PermissionService permissions) {
        this.produtos = produtos;
        this.productService = productService;
        this.estoque = estoque;
        this.movimentos = movimentos;
        this.inventario = inventario;
        this.sugestao = sugestao;
        this.permissions = permissions;
    }

    @GetMapping
    public ApiResponse<?> list() {
        permissions.page("produtos");
        return ApiResponse.ok(productService.list("", 0, 100));
    }

    @GetMapping("/resumo")
    public ApiResponse<?> resumo() {
        permissions.page("produtos");
        var low = produtos.lowStock();
        return ApiResponse.ok(Map.of("baixoOuEsgotado", low.size(), "produtos", low.stream().map(productService::response).toList()));
    }

    @GetMapping("/baixo")
    public ApiResponse<?> baixo() {
        permissions.page("produtos");
        return ApiResponse.ok(produtos.lowStock().stream().map(productService::response).toList());
    }

    // Movimentações com filtros opcionais; sem filtros devolve as mais recentes.
    @GetMapping("/movimentos")
    public ApiResponse<PageResponse<MovimentoResponse>> movimentos(@RequestParam(required = false) String produtoId, @RequestParam(required = false) String saldo,
                                                                   @RequestParam(required = false) String tipo, @RequestParam(required = false) LocalDate inicio,
                                                                   @RequestParam(required = false) LocalDate fim, @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "50") int size) {
        permissions.page("produtos");
        return ApiResponse.ok(PageResponse.from(movimentos.buscar(new MovimentoService.Filtro(produtoId, saldo, tipo, inicio, fim), page, size)));
    }

    @PostMapping("/ajuste")
    @Transactional
    public ApiResponse<?> ajustar(@Valid @RequestBody AjusteRequest r) {
        permissions.action("estoque_ajustar");
        if (r.quantidade().signum() == 0)
            throw new BusinessException("QUANTIDADE_INVALIDA", "A quantidade do ajuste não pode ser zero", HttpStatus.UNPROCESSABLE_ENTITY);
        var p = productService.locked(r.produtoId());
        estoque.change(p, r.saldo(), r.quantidade(), "AJUSTE_MANUAL", "AJUSTE", UUID.randomUUID().toString(), r.observacao());
        return ApiResponse.ok(productService.response(p));
    }

    @PostMapping("/inventario")
    public ApiResponse<InventarioResumo> inventario(@Valid @RequestBody InventarioRequest r) {
        permissions.action("estoque_ajustar");
        return ApiResponse.ok(inventario.aplicar(r));
    }

    @GetMapping("/sugestao-compra")
    public ApiResponse<List<SugestaoCompraService.SugestaoItem>> sugestaoCompra() {
        permissions.page("produtos");
        return ApiResponse.ok(sugestao.calcular());
    }
}

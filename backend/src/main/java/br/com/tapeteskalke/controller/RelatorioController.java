package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.dto.ApiResponse;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.service.*;
import java.time.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioController {
    // Período dos relatórios: padrão = início do mês até hoje; início depois do fim → 400.
    public record Periodo(LocalDate inicio, LocalDate fim) {}

    private final RelatorioService service;
    private final RelatorioAnaliticoService analitico;
    private final RelatorioResumoService resumo;
    private final PermissionService permissions;
    private final Clock clock;

    public RelatorioController(RelatorioService service, RelatorioAnaliticoService analitico, RelatorioResumoService resumo, PermissionService permissions, Clock clock) {
        this.resumo = resumo;
        this.service = service;
        this.analitico = analitico;
        this.permissions = permissions;
        this.clock = clock;
    }

    public Periodo periodo(LocalDate inicio, LocalDate fim) {
        permissions.action("relatorios_visualizar");
        var today = LocalDate.now(clock);
        var start = inicio == null ? today.withDayOfMonth(1) : inicio;
        var end = fim == null ? today : fim;
        if (start.isAfter(end))
            throw new BusinessException("PERIODO_INVALIDO", "A data inicial deve ser anterior ou igual à data final", HttpStatus.BAD_REQUEST);
        return new Periodo(start, end);
    }

    @GetMapping("/resumo")
    public ApiResponse<RelatorioResumoService.Resumo> resumo(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        var p = periodo(inicio, fim);
        return ApiResponse.ok(resumo.resumo(p.inicio(), p.fim()));
    }

    @GetMapping("/financeiro")
    public ApiResponse<?> financeiro(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        var p = periodo(inicio, fim);
        return ApiResponse.ok(service.financial(p.inicio(), p.fim()));
    }

    @GetMapping("/curva-abc")
    public ApiResponse<List<RelatorioAnaliticoService.AbcItem>> curvaAbc(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        var p = periodo(inicio, fim);
        return ApiResponse.ok(analitico.curvaAbc(p.inicio(), p.fim()));
    }

    @GetMapping("/margem-produtos")
    public ApiResponse<List<RelatorioAnaliticoService.MargemItem>> margem(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        var p = periodo(inicio, fim);
        return ApiResponse.ok(analitico.margem(p.inicio(), p.fim()));
    }

    @GetMapping("/vendas-por-vendedor")
    public ApiResponse<List<RelatorioAnaliticoService.VendedorItem>> vendedores(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
        var p = periodo(inicio, fim);
        return ApiResponse.ok(analitico.porVendedor(p.inicio(), p.fim()));
    }
}

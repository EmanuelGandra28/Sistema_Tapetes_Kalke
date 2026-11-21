package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class RelatorioResumoServiceTest {
    private final VendaRepository vendas = mock(VendaRepository.class);
    private final RelatorioService financeiro = mock(RelatorioService.class);
    private final RelatorioResumoService service = new RelatorioResumoService(vendas, financeiro);

    private static Map<String, BigDecimal> fin(String receita, String bruto, String liquido, String salarios, String outras) {
        var m = new LinkedHashMap<String, BigDecimal>();
        m.put("receita", new BigDecimal(receita));
        m.put("lucroBruto", new BigDecimal(bruto));
        m.put("lucroLiquido", new BigDecimal(liquido));
        m.put("salarios", new BigDecimal(salarios));
        m.put("outrasDespesas", new BigDecimal(outras));
        return m;
    }

    @Test
    void periodoAnteriorTemOMesmoNumeroDeDiasELogoAntes() {
        // 11 a 20 de novembro = 10 dias → anterior é 1 a 10 de novembro
        var p = RelatorioResumoService.periodoAnterior(LocalDate.parse("2026-11-11"), LocalDate.parse("2026-11-20"));
        assertEquals(LocalDate.parse("2026-11-01"), p[0]);
        assertEquals(LocalDate.parse("2026-11-10"), p[1]);
        // um dia só
        var um = RelatorioResumoService.periodoAnterior(LocalDate.parse("2026-11-05"), LocalDate.parse("2026-11-05"));
        assertEquals(LocalDate.parse("2026-11-04"), um[0]);
        assertEquals(LocalDate.parse("2026-11-04"), um[1]);
    }

    @Test
    void diasSemVendaViramZero() {
        var totais = Map.of(LocalDate.parse("2026-11-02"), new BigDecimal("150.00"));
        var dias = RelatorioResumoService.preencherDias(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"), totais);
        assertEquals(3, dias.size());
        assertEquals(BigDecimal.ZERO, dias.get(0).total());
        assertEquals(new BigDecimal("150.00"), dias.get(1).total());
        assertEquals(BigDecimal.ZERO, dias.get(2).total());
    }

    @Test
    void resumoTrazIndicadoresDoPeriodoEDoAnteriorComTicketMedio() {
        var de = LocalDate.parse("2026-11-11");
        var ate = LocalDate.parse("2026-11-20");
        when(financeiro.financial(de, ate)).thenReturn(fin("1000.00", "400.00", "250.00", "100.00", "50.00"));
        when(financeiro.financial(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-10"))).thenReturn(fin("500.00", "200.00", "100.00", "60.00", "40.00"));
        when(vendas.countFinalized(de, ate)).thenReturn(4L);
        when(vendas.countFinalized(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-10"))).thenReturn(0L);
        when(vendas.dailyTotals(any(), any())).thenReturn(List.of());
        when(vendas.totaisPorForma(any(), any())).thenReturn(List.<Object[]>of(new Object[]{"PIX", 3L, new BigDecimal("700.00")}));
        when(vendas.topProdutos(any(), any(), any())).thenReturn(List.<Object[]>of(new Object[]{"Tapete A", new BigDecimal("2"), new BigDecimal("600.00")}));

        var r = service.resumo(de, ate);
        assertEquals(new BigDecimal("250.00"), r.atual().ticketMedio());
        assertEquals(new BigDecimal("150.00"), r.atual().despesas());
        assertEquals(BigDecimal.ZERO, r.anterior().ticketMedio());
        assertEquals(10, r.porDia().size());
        assertEquals("PIX", r.porForma().get(0).forma());
        assertEquals("Tapete A", r.maisVendidos().get(0).produto());
    }
}

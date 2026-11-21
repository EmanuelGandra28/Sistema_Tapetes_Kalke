package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.dto.OperacaoDtos.DespesaRequest;
import br.com.tapeteskalke.entity.Despesa;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.DespesaRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DespesaServiceTest {
    private final DespesaRepository repo = mock(DespesaRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final DespesaService service = new DespesaService(repo, audit, clock);

    private Despesa existente() {
        var d = new Despesa();
        d.setId("d1");
        d.setTipo("OUTROS");
        d.setDescricao("Luz");
        d.setValor(new BigDecimal("100.00"));
        d.setDataDespesa(LocalDate.parse("2026-11-10"));
        return d;
    }

    @Test
    void editarAlteraOsCamposDaDespesa() {
        var d = existente();
        when(repo.findById("d1")).thenReturn(Optional.of(d));
        when(repo.save(any(Despesa.class))).thenAnswer(i -> i.getArgument(0));
        var r = new DespesaRequest("FRETES", "Frete da semana", new BigDecimal("250.50"), LocalDate.parse("2026-11-12"), "obs");
        var salvo = service.update("d1", r);
        assertEquals("FRETES", salvo.getTipo());
        assertEquals(new BigDecimal("250.50"), salvo.getValor());
        assertEquals("Frete da semana", salvo.getDescricao());
        verify(audit).changed(d);
    }

    @Test
    void excluirGravaMotivoQuemEQuandoSemApagarOLancamento() {
        var d = existente();
        when(repo.findById("d1")).thenReturn(Optional.of(d));
        when(audit.userId()).thenReturn("u1");
        service.delete("d1", "  Lançada em duplicidade ");
        assertNotNull(d.getExcluidaEm());
        assertEquals("u1", d.getExcluidaPor());
        assertEquals("Lançada em duplicidade", d.getMotivoExclusao());
        verify(repo, never()).delete(any(Despesa.class));
    }

    @Test
    void excluirExigeMotivo() {
        var e = assertThrows(BusinessException.class, () -> service.delete("d1", "  "));
        assertEquals("MOTIVO_OBRIGATORIO", e.code());
        verify(repo, never()).findById(any());
    }

    @Test
    void despesaJaExcluidaNaoPodeSerEditadaNemExcluidaDeNovo() {
        var d = existente();
        d.setExcluidaEm(OffsetDateTime.now(clock));
        when(repo.findById("d1")).thenReturn(Optional.of(d));
        assertThrows(BusinessException.class, () -> service.delete("d1", "x"));
        assertThrows(BusinessException.class, () -> service.update("d1", new DespesaRequest("OUTROS", "a", BigDecimal.ONE, LocalDate.now(clock), null)));
    }
}

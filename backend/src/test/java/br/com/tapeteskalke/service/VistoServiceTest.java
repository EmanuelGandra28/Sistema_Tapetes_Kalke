package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.UsuarioVisto;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.UsuarioVistoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class VistoServiceTest {
    private final UsuarioVistoRepository repo = mock(UsuarioVistoRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final br.com.tapeteskalke.repository.UsuarioRepository usuarios = mock(br.com.tapeteskalke.repository.UsuarioRepository.class);
    private final VistoService service = new VistoService(repo, audit, usuarios);

    private static UsuarioVisto visto(String chave) {
        var v = new UsuarioVisto();
        v.setChave(chave);
        return v;
    }

    @Test
    void listaSoOQueOUsuarioLogadoViu() {
        when(audit.userId()).thenReturn("u1");
        when(repo.findAllByIdUsuario("u1")).thenReturn(List.of(visto("ajuda:vendas"), visto("ajuda:clientes")));
        assertEquals(List.of("ajuda:clientes", "ajuda:vendas"), service.listar());
    }

    @Test
    void marcarGravaUmaVezEIgnoraRepeticao() {
        when(audit.userId()).thenReturn("u1");
        service.marcar("ajuda:vendas");
        verify(repo).save(any(UsuarioVisto.class));
        when(repo.existsByIdUsuarioAndChave("u1", "ajuda:vendas")).thenReturn(true);
        service.marcar("ajuda:vendas");
        verify(repo, times(1)).save(any(UsuarioVisto.class));
    }

    @Test
    void recusaChaveEPrefixoForaDoPadrao() {
        for (var ruim : new String[]{null, "", "vendas", "ajuda:", "AJUDA:x", "ajuda:a b", "ajuda:" + "x".repeat(60)})
            assertThrows(BusinessException.class, () -> service.marcar(ruim), String.valueOf(ruim));
        for (var ruim : new String[]{null, "ajuda", "ajuda:vendas", "%"})
            assertThrows(BusinessException.class, () -> service.limpar(ruim), String.valueOf(ruim));
        verify(repo, never()).save(any());
    }

    @Test
    void reverTutoriaisApagaSoOsDoUsuarioLogadoComOPrefixo() {
        when(audit.userId()).thenReturn("u1");
        var a = visto("ajuda:vendas");
        when(repo.findAllByIdUsuarioAndChaveStartingWith("u1", "ajuda:")).thenReturn(List.of(a));
        service.limpar("ajuda:");
        verify(repo).deleteAll(List.of(a));
        verify(repo, never()).findAllByIdUsuarioAndChaveStartingWith(eq("u2"), any());
    }

    @Test
    void contaCriadaEmVemDoUsuarioLogado() {
        when(audit.userId()).thenReturn("u1");
        var u = new br.com.tapeteskalke.entity.Usuario();
        u.setCreatedAt(java.time.OffsetDateTime.parse("2026-01-10T12:00:00Z"));
        when(usuarios.findById("u1")).thenReturn(java.util.Optional.of(u));
        assertEquals(java.time.OffsetDateTime.parse("2026-01-10T12:00:00Z"), service.contaCriadaEm());
        when(usuarios.findById("u1")).thenReturn(java.util.Optional.empty());
        assertNull(service.contaCriadaEm());
    }
}

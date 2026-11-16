package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.exception.AccessDeniedBusinessException;
import br.com.tapeteskalke.repository.PermissaoRepository;
import br.com.tapeteskalke.security.CurrentUser;
import br.com.tapeteskalke.security.UserPrincipal;
import org.junit.jupiter.api.Test;

class PermissionServiceTest {
    private final CurrentUser current = mock(CurrentUser.class);
    private final PermissaoRepository repo = mock(PermissaoRepository.class);
    private final PermissionService service = new PermissionService(current, repo);

    @Test
    void administradorTemTodasAsPaginasEAcoesSemConsultarATabela() {
        when(current.require()).thenReturn(new UserPrincipal("a1", "adm@x.com", "ADMINISTRADOR"));
        assertDoesNotThrow(() -> service.page("configuracoes"));
        assertDoesNotThrow(() -> service.action("vendas_cancelar"));
        assertTrue(service.pages().values().stream().allMatch(Boolean::booleanValue));
        assertTrue(service.actions().values().stream().allMatch(Boolean::booleanValue));
        verify(repo, never()).hasPage(anyString(), anyString());
        verify(repo, never()).hasAction(anyString(), anyString());
    }

    @Test
    void outrosCargosSeguemATabela() {
        when(current.require()).thenReturn(new UserPrincipal("v1", "v@x.com", "VENDEDOR"));
        when(repo.hasAction("VENDEDOR", "vendas_cancelar")).thenReturn(false);
        assertThrows(AccessDeniedBusinessException.class, () -> service.action("vendas_cancelar"));
    }
}

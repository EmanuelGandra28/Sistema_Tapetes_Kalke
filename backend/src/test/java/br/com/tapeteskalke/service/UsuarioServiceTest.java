package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.dto.UsuarioDtos.UpdateRequest;
import br.com.tapeteskalke.entity.Cargo;
import br.com.tapeteskalke.entity.Usuario;
import br.com.tapeteskalke.exception.AccessDeniedBusinessException;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.CargoRepository;
import br.com.tapeteskalke.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceTest {
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final CargoRepository cargos = mock(CargoRepository.class);
    private final CurrentRole role = mock(CurrentRole.class);
    private final UsuarioService service = new UsuarioService(users, cargos, mock(PasswordEncoder.class), mock(AuditService.class), role);
    private Usuario dono;

    private static Cargo cargo(String codigo) {
        var c = new Cargo();
        c.setCodigo(codigo);
        return c;
    }

    @BeforeEach
    void setUp() {
        when(role.value()).thenReturn("DONO");
        when(cargos.findByCodigo("DONO")).thenReturn(Optional.of(cargo("DONO")));
        when(cargos.findByCodigo("GERENTE")).thenReturn(Optional.of(cargo("GERENTE")));
        dono = new Usuario();
        dono.setNome("Dono");
        dono.setCargo(cargo("DONO"));
        when(users.findById("d1")).thenReturn(Optional.of(dono));
    }

    @Test
    void naoDesativaOUltimoDono() {
        when(users.countActiveOwners()).thenReturn(1L);
        var e = assertThrows(BusinessException.class, () -> service.update("d1", new UpdateRequest("Dono", "DONO", false)));
        assertEquals("SELF_LOCKOUT", e.code());
        assertTrue(dono.isAtivo());
    }

    @Test
    void naoRebaixaOUltimoDono() {
        when(users.countActiveOwners()).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.update("d1", new UpdateRequest("Dono", "GERENTE", true)));
        assertEquals("DONO", dono.getCargo().getCodigo());
    }

    @Test
    void desativaDonoQuandoExisteOutro() {
        when(users.countActiveOwners()).thenReturn(2L);
        service.update("d1", new UpdateRequest("Dono", "DONO", false));
        assertFalse(dono.isAtivo());
    }

    @Test
    void somenteDonoGerenciaUsuarios() {
        when(role.value()).thenReturn("GERENTE");
        assertThrows(AccessDeniedBusinessException.class, service::list);
    }
}

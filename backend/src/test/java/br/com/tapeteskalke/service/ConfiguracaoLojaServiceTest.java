package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.ConfiguracaoLoja;
import br.com.tapeteskalke.repository.ConfiguracaoLojaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfiguracaoLojaServiceTest {
    private final ConfiguracaoLojaRepository repo = mock(ConfiguracaoLojaRepository.class);
    private final ConfiguracaoLojaService service = new ConfiguracaoLojaService(repo);

    @Test
    void semLinhaDevolveDadosVazios() {
        when(repo.findById("LOJA")).thenReturn(Optional.empty());
        var d = service.dados();
        assertNull(d.pixChave());
        assertFalse(d.pixConfigurado());
    }

    @Test
    void salvarAparaEspacosETrocaVazioPorNulo() {
        when(repo.findById("LOJA")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var d = service.salvar(new ConfiguracaoLojaService.Dados("  loja@kalke.com.br ", " Tapetes Kalke ", "  ", false));
        assertEquals("loja@kalke.com.br", d.pixChave());
        assertEquals("Tapetes Kalke", d.pixNomeRecebedor());
        assertNull(d.pixCidade());
        assertFalse(d.pixConfigurado());
        verify(repo).save(argThat((ConfiguracaoLoja c) -> "LOJA".equals(c.getId())));
    }
}

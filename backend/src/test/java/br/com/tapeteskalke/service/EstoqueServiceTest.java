package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.MovimentoEstoque;
import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.MovimentoEstoqueRepository;
import br.com.tapeteskalke.security.CurrentUser;
import br.com.tapeteskalke.security.UserPrincipal;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EstoqueServiceTest {
    private final MovimentoEstoqueRepository movimentos = mock(MovimentoEstoqueRepository.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final EstoqueService service = new EstoqueService(movimentos, current);
    private Produto produto;

    @BeforeEach
    void setUp() {
        when(current.require()).thenReturn(new UserPrincipal("u1", "a@b.com", "DONO"));
        produto = new Produto();
        produto.setNome("Tapete");
        produto.setEstoqueComercial(new BigDecimal("5.000"));
        produto.setEstoqueProducao(new BigDecimal("2.000"));
    }

    @Test
    void saidaMaiorQueSaldoGeraConflitoSemGravarMovimento() {
        var e = assertThrows(BusinessException.class, () -> service.change(produto, "COMERCIAL", new BigDecimal("-6"), "SAIDA_VENDA", "VENDA", "v1", null));
        assertEquals("ESTOQUE_INSUFICIENTE", e.code());
        assertEquals(new BigDecimal("5.000"), produto.getEstoqueComercial());
        verifyNoInteractions(movimentos);
    }

    @Test
    void movimentoRegistraSaldoAnteriorEPosteriorNoSaldoCerto() {
        service.change(produto, "PRODUCAO", new BigDecimal("-2"), "CONSUMO_PRODUCAO", "PRODUCAO", "o1", null);
        assertEquals(0, produto.getEstoqueProducao().signum());
        assertEquals(new BigDecimal("5.000"), produto.getEstoqueComercial());
        var captor = ArgumentCaptor.forClass(MovimentoEstoque.class);
        verify(movimentos).save(captor.capture());
        assertEquals(new BigDecimal("2.000"), captor.getValue().getEstoqueAnterior());
        assertEquals(0, captor.getValue().getEstoquePosterior().signum());
        assertEquals("u1", captor.getValue().getIdUsuario());
    }
}

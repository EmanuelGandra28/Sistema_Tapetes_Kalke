package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.OrdemProducao;
import br.com.tapeteskalke.entity.OrdemProducaoItem;
import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.repository.OrdemProducaoRepository;
import br.com.tapeteskalke.repository.ProdutoComponenteRepository;
import br.com.tapeteskalke.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Abre uma ordem de produção sozinha quando o estoque comercial de um produto fica baixo ou esgota.
 * Roda depois do commit da operação que baixou o estoque e numa transação própria: se algo falhar aqui,
 * a venda (ou ajuste) já está gravada e não é desfeita; o problema fica só no log.
 */
@Service
public class ReposicaoAutomaticaService {
    private static final Logger log = LoggerFactory.getLogger(ReposicaoAutomaticaService.class);
    private final ProdutoService produtos;
    private final ProdutoComponenteRepository fichas;
    private final OrdemProducaoRepository ordens;
    private final NumeroService numeros;
    private final CurrentUser current;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public ReposicaoAutomaticaService(ProdutoService produtos, ProdutoComponenteRepository fichas, OrdemProducaoRepository ordens, NumeroService numeros, CurrentUser current, ApplicationEventPublisher eventos, Clock clock) {
        this.produtos = produtos;
        this.fichas = fichas;
        this.ordens = ordens;
        this.numeros = numeros;
        this.current = current;
        this.eventos = eventos;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoFicarBaixo(EventosEstoque.EstoqueComercialBaixo evento) {
        try {
            criarSeNecessario(evento.produtoId());
        } catch (RuntimeException e) {
            log.error("Falha ao criar ordem de produção automática para o produto {}", evento.produtoId(), e);
        }
    }

    /** Devolve a ordem criada, ou vazio quando não há o que fazer (saldo já reposto, ordem aberta ou produto sem ficha técnica). */
    @Transactional
    public Optional<OrdemProducao> criarSeNecessario(String produtoId) {
        // A trava no produto impede que duas vendas simultâneas abram duas ordens para o mesmo tapete.
        Produto p = produtos.locked(produtoId);
        if (!p.isAtivo() || !p.isPodeSerVendido()) return Optional.empty();
        if (p.getEstoqueComercial().compareTo(p.getEstoqueMinimoComercial()) > 0) return Optional.empty();
        if (ordens.existsByProdutoAcabadoIdAndStatus(p.getId(), "ABERTA")) return Optional.empty();
        var ficha = fichas.fichaDe(p.getId());
        if (ficha.isEmpty()) {
            log.warn("Produto {} ({}) está com estoque baixo, mas não tem ficha técnica: ordem automática não criada", p.getNome(), p.getId());
            return Optional.empty();
        }
        BigDecimal quantidade = quantidadeReposicao(p.getEstoqueComercial(), p.getEstoqueMinimoComercial(), p.getUnidadeMedida());
        var o = new OrdemProducao();
        o.setNumeroOrdem(numeros.nextOrdem());
        o.setProdutoAcabado(p);
        o.setQuantidadeProduzida(quantidade);
        o.setTipoOrdem("ESTOQUE");
        o.setOrigem("AUTOMATICA");
        o.setDataOrdem(LocalDate.now(clock));
        o.setObservacoes("Criada automaticamente: estoque comercial em " + p.getEstoqueComercial().stripTrailingZeros().toPlainString()
                + " (mínimo " + p.getEstoqueMinimoComercial().stripTrailingZeros().toPlainString() + ").");
        String usuario = usuarioAtual();
        o.setCreatedBy(usuario);
        o.setUpdatedBy(usuario);
        for (var c : ficha) {
            var item = new OrdemProducaoItem();
            item.setProdutoMateriaPrima(c.getMateriaPrima());
            item.setQuantidadeUsada(c.getQuantidade().multiply(quantidade).setScale(3, RoundingMode.HALF_UP));
            item.setProdutoNomeSnapshot(c.getMateriaPrima().getNome());
            item.setUnidadeSnapshot(c.getMateriaPrima().getUnidadeMedida());
            o.addItem(item);
        }
        ordens.save(o);
        log.info("Ordem de produção automática #{} aberta: {} x {}", o.getNumeroOrdem(), quantidade.toPlainString(), p.getNome());
        eventos.publishEvent(new EventosEstoque.OrdemAutomaticaCriada(o.getId(), o.getNumeroOrdem(), p.getNome(), quantidade, p.getEstoqueComercial(), p.getEstoqueMinimoComercial()));
        return Optional.of(o);
    }

    /**
     * Quanto produzir para repor: leva o saldo até o dobro do estoque mínimo (ou mínimo + 1, se o mínimo for 0 ou 1).
     * Produtos vendidos por unidade são arredondados para cima, porque não se produz meio tapete.
     */
    static BigDecimal quantidadeReposicao(BigDecimal saldo, BigDecimal minimo, String unidade) {
        BigDecimal alvo = minimo.multiply(BigDecimal.valueOf(2)).max(minimo.add(BigDecimal.ONE));
        BigDecimal falta = alvo.subtract(saldo);
        if (falta.signum() <= 0) falta = BigDecimal.ONE;
        return "UNIDADE".equals(unidade) ? falta.setScale(0, RoundingMode.CEILING) : falta.setScale(3, RoundingMode.HALF_UP);
    }

    private String usuarioAtual() {
        try {
            return current.require().id();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

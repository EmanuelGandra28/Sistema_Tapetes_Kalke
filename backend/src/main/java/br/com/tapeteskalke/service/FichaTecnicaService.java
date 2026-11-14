package br.com.tapeteskalke.service;

import br.com.tapeteskalke.dto.ProdutoDtos.ComponenteResponse;
import br.com.tapeteskalke.dto.ProdutoDtos.FichaTecnicaRequest;
import br.com.tapeteskalke.dto.ProdutoDtos.FichaTecnicaResponse;
import br.com.tapeteskalke.entity.ProdutoComponente;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.ProdutoComponenteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ficha técnica (receita) de um produto acabado: matérias-primas e quantidades por unidade produzida. */
@Service
public class FichaTecnicaService {
    private final ProdutoComponenteRepository fichas;
    private final ProdutoService produtos;

    public FichaTecnicaService(ProdutoComponenteRepository fichas, ProdutoService produtos) {
        this.fichas = fichas;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public FichaTecnicaResponse get(String produtoId) {
        var produto = produtos.active(produtoId);
        var itens = fichas.fichaDe(produtoId).stream()
                .map(c -> new ComponenteResponse(c.getMateriaPrima().getId(), c.getMateriaPrima().getNome(), c.getMateriaPrima().getUnidadeMedida(),
                        c.getQuantidade(), c.getMateriaPrima().getEstoqueProducao(), c.getMateriaPrima().getPrecoCusto()))
                .toList();
        // Custo de matéria-prima de uma unidade, a partir do preço de custo cadastrado de cada componente.
        BigDecimal custo = itens.stream().map(i -> i.quantidade().multiply(i.precoCusto())).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new FichaTecnicaResponse(produto.getId(), produto.getNome(), itens, custo);
    }

    /** Substitui a ficha inteira (lista vazia apaga a ficha). */
    @Transactional
    public FichaTecnicaResponse salvar(String produtoId, FichaTecnicaRequest r) {
        var produto = produtos.active(produtoId);
        var vistos = new HashSet<String>();
        var novos = r.componentes().stream().map(c -> {
            if (c.materiaPrimaId().equals(produtoId)) throw invalido("O produto não pode ser matéria-prima de si mesmo");
            if (!vistos.add(c.materiaPrimaId())) throw invalido("Matéria-prima repetida na ficha técnica");
            var materia = produtos.active(c.materiaPrimaId());
            if (!materia.isPodeSerMateriaPrima()) throw invalido(materia.getNome() + " não está marcado como matéria-prima");
            var linha = new ProdutoComponente();
            linha.setProduto(produto);
            linha.setMateriaPrima(materia);
            linha.setQuantidade(c.quantidade().setScale(3, RoundingMode.HALF_UP));
            return linha;
        }).toList();
        fichas.apagarFicha(produtoId);
        fichas.flush();
        fichas.saveAll(novos);
        return get(produtoId);
    }

    private BusinessException invalido(String mensagem) {
        return new BusinessException("FICHA_TECNICA_INVALIDA", mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

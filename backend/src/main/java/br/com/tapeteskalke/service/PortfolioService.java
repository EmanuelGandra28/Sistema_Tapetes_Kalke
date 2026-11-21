package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.repository.ProdutoRepository;
import br.com.tapeteskalke.service.ProdutoImagemService.ImagemResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Catálogo público para o site de portfólio. Só devolve campos seguros: nunca custo, estoque, código de barras,
// fornecedor ou observações. O preço só sai para tapetes "à venda" no site e com valor maior que zero.
@Service
public class PortfolioService {
    private static final Set<String> MODOS_PUBLICOS = Set.of("VENDA", "GALERIA");

    public record ItemPublico(String id, String nome, String linha, String descricao, String cor, String tamanho, BigDecimal preco,
                              String selo, int ordem, String modo, List<ImagemResponse> fotos) {}

    public record CatalogoPublico(String geradoEm, List<ItemPublico> produtos) {}

    private final ProdutoRepository repo;
    private final ProdutoImagemService imagens;
    private final Clock clock;

    public PortfolioService(ProdutoRepository repo, ProdutoImagemService imagens, Clock clock) {
        this.repo = repo;
        this.imagens = imagens;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CatalogoPublico catalogo() {
        var itens = repo.portfolio().stream()
                .filter(p -> p.getDeletedAt() == null && p.isAtivo() && "TAPETE".equals(p.getTipo()) && MODOS_PUBLICOS.contains(p.getPortfolioModo()))
                .map(this::item)
                .toList();
        return new CatalogoPublico(Instant.now(clock).toString(), itens);
    }

    private ItemPublico item(Produto p) {
        var comPreco = "VENDA".equals(p.getPortfolioModo()) && p.getPrecoVenda() != null && p.getPrecoVenda().signum() > 0;
        return new ItemPublico(p.getId(), p.getNome(), p.getPortfolioLinha(), p.getPortfolioDescricao(), p.getCor(), p.getTamanho(),
                comPreco ? p.getPrecoVenda() : null, p.getPortfolioSelo(), p.getPortfolioOrdem(), p.getPortfolioModo(), imagens.listar(p.getId()));
    }
}

package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.ProdutoImagem;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.exception.ResourceNotFoundException;
import br.com.tapeteskalke.repository.ProdutoImagemRepository;
import br.com.tapeteskalke.repository.ProdutoImagemRepository.Resumo;
import br.com.tapeteskalke.repository.ProdutoRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Fotos dos produtos: valida o arquivo pelo conteúdo (não só pelo nome), limita tamanho e quantidade e mantém uma principal.
@Service
public class ProdutoImagemService {
    public static final int TAMANHO_MAXIMO = 2 * 1024 * 1024;
    public static final int MAXIMO_POR_PRODUTO = 8;

    public record ImagemResponse(String id, boolean principal, int ordem) {}

    private final ProdutoImagemRepository repo;
    private final ProdutoRepository produtos;
    private final AuditService audit;

    public ProdutoImagemService(ProdutoImagemRepository repo, ProdutoRepository produtos, AuditService audit) {
        this.repo = repo;
        this.produtos = produtos;
        this.audit = audit;
    }

    /** Tipo real da imagem pelos primeiros bytes (JPEG, PNG ou WEBP); null se não for uma imagem aceita. */
    public static String detectarTipo(byte[] b) {
        if (b == null || b.length < 12) return null;
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    @Transactional(readOnly = true)
    public List<ImagemResponse> listar(String produtoId) {
        return repo.resumos(produtoId).stream().map(r -> new ImagemResponse(r.getId(), r.isPrincipal(), r.getOrdem())).toList();
    }

    // Mapa produto → id da foto principal, para as listas mostrarem miniatura com uma única consulta.
    @Transactional(readOnly = true)
    public Map<String, String> principais() {
        return repo.principais().stream().collect(Collectors.toMap(Resumo::getIdProduto, Resumo::getId, (a, b) -> a));
    }

    @Transactional
    public ImagemResponse adicionar(String produtoId, byte[] dados) {
        if (!produtos.existsById(produtoId)) throw new ResourceNotFoundException("Produto", produtoId);
        if (dados == null || dados.length == 0) throw new BusinessException("IMAGEM_VAZIA", "Escolha uma foto para enviar", HttpStatus.BAD_REQUEST);
        if (dados.length > TAMANHO_MAXIMO) throw new BusinessException("IMAGEM_GRANDE", "A foto é grande demais (máximo 2 MB)", HttpStatus.BAD_REQUEST);
        var tipo = detectarTipo(dados);
        if (tipo == null) throw new BusinessException("IMAGEM_INVALIDA", "Envie uma foto JPG, PNG ou WEBP", HttpStatus.BAD_REQUEST);
        var total = repo.contar(produtoId);
        if (total >= MAXIMO_POR_PRODUTO)
            throw new BusinessException("LIMITE_IMAGENS", "Cada produto aceita até " + MAXIMO_POR_PRODUTO + " fotos", HttpStatus.BAD_REQUEST);
        var i = new ProdutoImagem();
        i.setIdProduto(produtoId);
        i.setContentType(tipo);
        i.setDados(dados);
        i.setTamanho(dados.length);
        i.setPrincipal(total == 0); // a primeira foto vira a principal
        i.setOrdem((int) total);
        audit.created(i);
        i = repo.save(i);
        return new ImagemResponse(i.getId(), i.isPrincipal(), i.getOrdem());
    }

    @Transactional
    public void definirPrincipal(String produtoId, String imagemId) {
        var alvo = repo.doProduto(produtoId, imagemId).orElseThrow(() -> new ResourceNotFoundException("Imagem", imagemId));
        repo.limparPrincipal(produtoId);
        repo.flush();
        alvo.setPrincipal(true);
        audit.changed(alvo);
        repo.save(alvo);
    }

    @Transactional
    public void remover(String produtoId, String imagemId) {
        var alvo = repo.doProduto(produtoId, imagemId).orElseThrow(() -> new ResourceNotFoundException("Imagem", imagemId));
        var eraPrincipal = alvo.isPrincipal();
        repo.delete(alvo);
        repo.flush();
        // Se a principal saiu, a primeira que sobrar assume.
        if (eraPrincipal) repo.todas(produtoId).stream().findFirst().ifPresent(n -> { n.setPrincipal(true); audit.changed(n); repo.save(n); });
    }

    @Transactional(readOnly = true)
    public ProdutoImagem conteudo(String imagemId) {
        return repo.findById(imagemId).orElseThrow(() -> new ResourceNotFoundException("Imagem", imagemId));
    }
}

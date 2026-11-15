package br.com.tapeteskalke.service;

import br.com.tapeteskalke.dto.OperacaoDtos.*;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Aplica uma contagem física: cada diferença vira um movimento INVENTARIO, todos com o mesmo id de contagem.
@Service
public class InventarioService {
    private final ProdutoService produtos;
    private final EstoqueService estoque;

    public InventarioService(ProdutoService produtos, EstoqueService estoque) {
        this.produtos = produtos;
        this.estoque = estoque;
    }

    @Transactional
    public InventarioResumo aplicar(InventarioRequest r) {
        var contagem = UUID.randomUUID().toString();
        var nota = r.observacao() == null || r.observacao().isBlank() ? "Inventário" : r.observacao().trim();
        var diferencas = new ArrayList<InventarioDiferenca>();
        for (var item : r.itens()) {
            var p = produtos.locked(item.produtoId());
            var sistema = "COMERCIAL".equals(r.saldo()) ? p.getEstoqueComercial() : p.getEstoqueProducao();
            var diferenca = item.contado().subtract(sistema);
            if (diferenca.signum() == 0) continue;
            estoque.change(p, r.saldo(), diferenca, "INVENTARIO", "INVENTARIO", contagem, nota);
            diferencas.add(new InventarioDiferenca(p.getNome(), sistema, item.contado(), diferenca));
        }
        return new InventarioResumo(r.itens().size(), diferencas.size(), diferencas);
    }

}

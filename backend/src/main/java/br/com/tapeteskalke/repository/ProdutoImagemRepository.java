package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.ProdutoImagem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProdutoImagemRepository extends JpaRepository<ProdutoImagem, String> {
    // Listagens usam projeção para não carregar os bytes das fotos.
    interface Resumo { String getId(); String getIdProduto(); boolean isPrincipal(); int getOrdem(); }

    @Query("select i.id as id, i.idProduto as idProduto, i.principal as principal, i.ordem as ordem from ProdutoImagem i where i.idProduto=:produto order by i.principal desc, i.ordem asc, i.createdAt asc")
    List<Resumo> resumos(@Param("produto") String produto);

    @Query("select i.id as id, i.idProduto as idProduto, i.principal as principal, i.ordem as ordem from ProdutoImagem i where i.principal=true")
    List<Resumo> principais();

    @Query("select count(i) from ProdutoImagem i where i.idProduto=:produto")
    long contar(@Param("produto") String produto);

    @Query("select i from ProdutoImagem i where i.idProduto=:produto and i.id=:id")
    Optional<ProdutoImagem> doProduto(@Param("produto") String produto, @Param("id") String id);

    @Query("select i from ProdutoImagem i where i.idProduto=:produto order by i.ordem asc, i.createdAt asc")
    List<ProdutoImagem> todas(@Param("produto") String produto);

    @Modifying
    @Query("update ProdutoImagem i set i.principal=false where i.idProduto=:produto and i.principal=true")
    void limparPrincipal(@Param("produto") String produto);
}

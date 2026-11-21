package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "produto_imagem")
@Getter
@Setter
public class ProdutoImagem extends BaseAuditEntity {
    @Column(name = "id_produto", nullable = false, length = 38)
    private String idProduto;
    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;
    @Column(nullable = false)
    private byte[] dados;
    @Column(nullable = false)
    private int tamanho;
    @Column(nullable = false)
    private boolean principal;
    @Column(nullable = false)
    private int ordem;
}

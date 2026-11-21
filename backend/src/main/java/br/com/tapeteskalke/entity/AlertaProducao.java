package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

// Aviso pendente (ou já enviado) à dona sobre uma ordem de produção automática.
@Entity
@Table(name = "alerta_producao")
@Getter
@Setter
public class AlertaProducao extends BaseAuditEntity {
    @Column(name = "id_ordem", nullable = false, length = 38, unique = true)
    private String idOrdem;
    @Column(name = "id_produto", nullable = false, length = 38)
    private String idProduto;
    @Column(name = "produto_nome", nullable = false, length = 180)
    private String produtoNome;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantidade;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal saldo;
    @Column(name = "estoque_minimo", nullable = false, precision = 15, scale = 3)
    private BigDecimal estoqueMinimo;
    @Column(nullable = false, length = 12)
    private String status = "PENDENTE";
    @Column(nullable = false)
    private int tentativas;
    @Column(name = "erro_codigo", length = 60)
    private String erroCodigo;
    @Column(length = 38)
    private String lote;
    @Column(name = "enviado_em")
    private OffsetDateTime enviadoEm;
}

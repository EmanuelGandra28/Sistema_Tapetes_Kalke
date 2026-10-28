package br.com.tapeteskalke.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "compra")
@Getter
@Setter
public class Compra extends BaseAuditEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fornecedor")
    private Fornecedor fornecedor;
    @Column(name = "fornecedor_nome_manual")
    private String fornecedorNomeManual;
    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;
    @Column(name = "total_custo", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCusto;
    @Column(nullable = false)
    private String status = "FINALIZADA";
    @Column(columnDefinition = "TEXT")
    private String observacoes;
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
    @Column(name = "cancelled_by")
    private String cancelledBy;
    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraItem> itens = new ArrayList<>();

    public void addItem(CompraItem item) {
        itens.add(item);
        item.setCompra(this);
    }
}
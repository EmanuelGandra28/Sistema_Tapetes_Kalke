package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "campanha_reativacao")
@Getter
@Setter
public class CampanhaReativacao extends BaseAuditEntity {
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, length = 20)
    private String status = "RASCUNHO";
    @Column(name = "intervalo_dias", nullable = false)
    private int intervaloDias = 5;
    @Column(name = "limite_diario", nullable = false)
    private int limiteDiario = 20;
    @Column(name = "aprovada_por", length = 38)
    private String aprovadaPor;
    @Column(name = "aprovada_em")
    private OffsetDateTime aprovadaEm;
}

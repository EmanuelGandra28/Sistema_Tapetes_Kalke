package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cliente_consentimento")
@Getter
@Setter
public class ClienteConsentimento extends BaseAuditEntity {
    @Column(name = "id_cliente", nullable = false, length = 38)
    private String idCliente;
    @Column(nullable = false, length = 60)
    private String finalidade;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String evidencia;
    @Column(nullable = false, length = 40)
    private String origem;
    @Column(name = "concedido_em", nullable = false)
    private OffsetDateTime concedidoEm;
    @Column(name = "revogado_em")
    private OffsetDateTime revogadoEm;
    @Column(name = "motivo_revogacao", length = 120)
    private String motivoRevogacao;
}

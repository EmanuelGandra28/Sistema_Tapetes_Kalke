package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

// Mensagem entregue ao gateway. Não guarda o texto (tem dado pessoal); guarda só o necessário para controle, cota e situação.
@Entity
@Table(name = "whatsapp_mensagem")
@Getter
@Setter
public class WhatsappMensagem extends BaseAuditEntity {
    @Column(name = "id_destinatario", length = 38)
    private String idDestinatario;
    @Column(nullable = false, length = 20)
    private String telefone;
    @Column(nullable = false)
    private int etapa;
    @Column(name = "idempotency_key", nullable = false, length = 80, unique = true)
    private String idempotencyKey;
    @Column(name = "mensagem_gateway", length = 80)
    private String mensagemGateway;
    @Column(nullable = false, length = 20)
    private String situacao = "na_fila";
    @Column(name = "erro_codigo", length = 60)
    private String erroCodigo;
    @Column(name = "enviado_em", nullable = false)
    private OffsetDateTime enviadoEm;
}

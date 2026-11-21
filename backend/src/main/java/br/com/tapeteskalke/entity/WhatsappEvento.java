package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "whatsapp_evento")
@Getter
@Setter
public class WhatsappEvento extends BaseAuditEntity {
    @Column(name = "evento_id", nullable = false, length = 80, unique = true)
    private String eventoId;
    @Column(nullable = false, length = 40)
    private String evento;
    @Column(name = "recebido_em", nullable = false)
    private OffsetDateTime recebidoEm;
}

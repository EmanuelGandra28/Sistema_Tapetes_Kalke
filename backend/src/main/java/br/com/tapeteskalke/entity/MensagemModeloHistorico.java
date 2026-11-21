package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Texto que estava valendo ANTES de uma alteração; createdBy/createdAt dizem quem alterou e quando.
@Entity
@Table(name = "mensagem_modelo_historico")
@Getter
@Setter
public class MensagemModeloHistorico extends BaseAuditEntity {
    @Column(nullable = false, length = 60)
    private String chave;
    @Column(name = "texto_anterior", nullable = false, columnDefinition = "TEXT")
    private String textoAnterior;
}

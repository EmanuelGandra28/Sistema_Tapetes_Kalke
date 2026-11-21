package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mensagem_modelo")
@Getter
@Setter
public class MensagemModelo extends BaseAuditEntity {
    @Column(nullable = false, length = 60, unique = true)
    private String chave;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;
    @Column(nullable = false)
    private boolean ativo = true;
}

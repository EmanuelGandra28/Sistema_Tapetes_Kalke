package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

// Linha única (id LOJA) da tabela configuracao com os dados do PIX da loja.
@Entity
@Table(name = "configuracao")
@Getter
@Setter
public class ConfiguracaoLoja {
    public static final String ID = "LOJA";
    @Id @Column(length = 38) private String id;
    @Column(name = "pix_chave", length = 77) private String pixChave;
    @Column(name = "pix_nome_recebedor", length = 25) private String pixNomeRecebedor;
    @Column(name = "pix_cidade", length = 15) private String pixCidade;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist @PreUpdate void tocar() { updatedAt = OffsetDateTime.now(); }
}

package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

// Um cliente dentro de uma campanha: em qual das 3 mensagens está, quando pode receber a próxima e como respondeu.
@Entity
@Table(name = "campanha_destinatario")
@Getter
@Setter
public class CampanhaDestinatario extends BaseAuditEntity {
    @Column(name = "id_campanha", nullable = false, length = 38)
    private String idCampanha;
    @Column(name = "id_cliente", nullable = false, length = 38)
    private String idCliente;
    @Column(name = "etapa_atual", nullable = false)
    private int etapaAtual;
    @Column(nullable = false, length = 20)
    private String status = "ATIVO";
    @Column(name = "proxima_etapa_em", nullable = false)
    private OffsetDateTime proximaEtapaEm;
    @Column(name = "primeiro_envio_em")
    private OffsetDateTime primeiroEnvioEm;
    @Column(name = "ultimo_envio_em")
    private OffsetDateTime ultimoEnvioEm;
    @Column(name = "resposta_classe", length = 20)
    private String respostaClasse;
    @Column(name = "resposta_texto", length = 500)
    private String respostaTexto;
    @Column(name = "resposta_em")
    private OffsetDateTime respostaEm;
}

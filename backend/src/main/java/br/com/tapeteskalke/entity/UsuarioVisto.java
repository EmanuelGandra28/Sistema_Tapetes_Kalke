package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario_visto")
@Getter
@Setter
public class UsuarioVisto extends BaseAuditEntity {
    @Column(name = "id_usuario", nullable = false, length = 38)
    private String idUsuario;
    @Column(nullable = false, length = 60)
    private String chave;
}

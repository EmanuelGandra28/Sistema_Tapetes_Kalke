package br.com.tapeteskalke.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cargo")
@Getter
@Setter
public class Cargo extends BaseAuditEntity {
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;
    @Column(nullable = false, length = 80)
    private String nome;
}
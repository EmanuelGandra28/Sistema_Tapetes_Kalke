package br.com.tapeteskalke.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ClienteDtos {
    private ClienteDtos() {
    }

    public record Request(@NotBlank @Size(max = 150) String nome, @NotBlank @Size(max = 20) String telefone,
                          @Email String email, @Size(max = 180) String razaoSocial, @Size(max = 18) String cnpj,
                          String endereco, String bairro, String cidade, @Size(max = 10) String cep,
                          LocalDate dataNascimento, String observacoes,
                          @Pattern(regexp = "ATIVO|INATIVO") String statusCadastral) {
    }

    public record Response(String id, String nome, String telefone, String email, String cidade, String statusCadastral,
                           String classificacao, LocalDate ultimaCompra, BigDecimal totalCompras, boolean ativo) {
    }
}
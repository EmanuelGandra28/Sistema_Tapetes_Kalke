package br.com.tapeteskalke.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FuncionarioDtos {
    private FuncionarioDtos() {
    }

    public record Request(@NotBlank String nome, String telefone, @Email String email, String endereco,
                          @DecimalMin("0.00") BigDecimal salario, LocalDate dataContratacao,
                          @Pattern(regexp = "ATIVO|INATIVO|FERIAS") String status, @NotBlank String cargoId,
                          String observacoes) {
    }

    public record Response(String id, String nome, String telefone, String email, String cargo, String status,
                           LocalDate dataContratacao, BigDecimal salario, String endereco, String observacoes) {
    }
}
package br.com.tapeteskalke.dto;

import jakarta.validation.constraints.*;

public final class FornecedorDtos {
    private FornecedorDtos() {
    }

    public record Request(@NotBlank @Size(max = 180) String nomeEmpresa, @Size(max = 18) String cnpj,
                          @NotBlank @Size(max = 20) String telefone, @Email String email, String observacoes) {
    }

    public record Response(String id, String nomeEmpresa, String cnpj, String telefone, String email, boolean ativo) {
    }
}
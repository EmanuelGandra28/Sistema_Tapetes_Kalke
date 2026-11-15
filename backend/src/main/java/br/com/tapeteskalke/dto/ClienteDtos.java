package br.com.tapeteskalke.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ClienteDtos {
    // Histórico de compras: os totais consideram só vendas finalizadas; a lista traz também as canceladas.
    public record Historico(Response cliente, BigDecimal totalFinalizado, long compras, BigDecimal ticketMedio,
                            LocalDate ultimaCompra, java.util.List<HistoricoVenda> vendas) {
    }

    public record HistoricoVenda(String id, Long numero, LocalDate data, String status, String formaPagamento,
                                 BigDecimal desconto, BigDecimal total, java.util.List<HistoricoItem> itens) {
    }

    public record HistoricoItem(String produto, BigDecimal quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    private ClienteDtos() {
    }

    public record Request(@NotBlank @Size(max = 150) String nome, @NotBlank @Size(max = 20) String telefone,
                          @Email String email, @Size(max = 180) String razaoSocial, @Size(max = 18) String cnpj,
                          String endereco, String bairro, String cidade, @Size(max = 10) String cep,
                          LocalDate dataNascimento, String observacoes,
                          @Pattern(regexp = "ATIVO|INATIVO") String statusCadastral) {
    }

    public record Response(String id, String nome, String telefone, String email, String cidade, String statusCadastral,
                           String classificacao, LocalDate ultimaCompra, BigDecimal totalCompras, boolean ativo,
                           // Demais campos do cadastro: a tela de edição precisa deles para não apagá-los ao salvar.
                           String razaoSocial, String cnpj, String endereco, String bairro, String cep,
                           LocalDate dataNascimento, String observacoes) {
    }
}
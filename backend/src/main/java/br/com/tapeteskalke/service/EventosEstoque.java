package br.com.tapeteskalke.service; import java.math.BigDecimal;
// Eventos internos do estoque/produção. São publicados pelo Spring dentro da transação e tratados depois do commit.
public final class EventosEstoque { private EventosEstoque(){}
 /** O saldo comercial de um produto caiu para o mínimo ou abaixo (venda, ajuste, estorno de compra). */
 public record EstoqueComercialBaixo(String produtoId){}
 /** O sistema abriu sozinho uma ordem de produção. Ponto de integração futuro: avisar a dona da loja pelo WhatsApp (gateway Orquestra). */
 public record OrdemAutomaticaCriada(String ordemId,Long numeroOrdem,String produto,BigDecimal quantidade,BigDecimal saldoComercial,BigDecimal estoqueMinimo){}
}

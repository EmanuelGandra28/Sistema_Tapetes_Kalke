package br.com.tapeteskalke.service; import br.com.tapeteskalke.exception.BusinessException; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service;
// Valida códigos (forma de pagamento, tipo de produto, unidade) contra as tabelas do schema utilitarios.
@Service public class CodigoService { public enum Tabela { FORMA_PAGAMENTO("forma_pagamento","Forma de pagamento"), TIPO_PRODUTO("tipo_produto","Tipo de produto"), UNIDADE_MEDIDA("unidade_medida","Unidade de medida"), TIPO_DESPESA("tipo_despesa","Tipo de despesa"); final String table; final String label; Tabela(String table,String label){this.table=table;this.label=label;} }
 private final JdbcTemplate jdbc; public CodigoService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public void require(Tabela t,String codigo){Boolean ok=jdbc.queryForObject("select exists(select 1 from utilitarios."+t.table+" where codigo=?)",Boolean.class,codigo);if(!Boolean.TRUE.equals(ok))throw new BusinessException("CODIGO_INVALIDO",t.label+" inválido(a): "+codigo,HttpStatus.UNPROCESSABLE_ENTITY);}
 public List<Map<String,Object>> list(Tabela t){return jdbc.queryForList("select codigo,nome from utilitarios."+t.table+" order by nome");}
}

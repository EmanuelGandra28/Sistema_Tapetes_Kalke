package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.MensagemModeloHistorico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemModeloHistoricoRepository extends JpaRepository<MensagemModeloHistorico, String> {
    List<MensagemModeloHistorico> findTop20ByChaveOrderByCreatedAtDesc(String chave);
}

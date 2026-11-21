package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.CampanhaReativacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaReativacaoRepository extends JpaRepository<CampanhaReativacao, String> {
    List<CampanhaReativacao> findAllByOrderByCreatedAtDesc();
}

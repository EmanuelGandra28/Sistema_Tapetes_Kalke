package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.AlertaProducao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaProducaoRepository extends JpaRepository<AlertaProducao, String> {
    List<AlertaProducao> findAllByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);

    boolean existsByIdOrdem(String idOrdem);
}

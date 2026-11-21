package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.MensagemModelo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemModeloRepository extends JpaRepository<MensagemModelo, String> {
    Optional<MensagemModelo> findByChave(String chave);

    List<MensagemModelo> findAllByOrderByChaveAsc();
}

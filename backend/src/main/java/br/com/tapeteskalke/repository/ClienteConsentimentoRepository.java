package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.ClienteConsentimento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteConsentimentoRepository extends JpaRepository<ClienteConsentimento, String> {
    Optional<ClienteConsentimento> findByIdClienteAndFinalidadeAndRevogadoEmIsNull(String idCliente, String finalidade);

    List<ClienteConsentimento> findAllByFinalidadeAndRevogadoEmIsNull(String finalidade);
}

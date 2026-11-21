package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.WhatsappEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappEventoRepository extends JpaRepository<WhatsappEvento, String> {
    boolean existsByEventoId(String eventoId);
}

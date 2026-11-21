package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.WhatsappMensagem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WhatsappMensagemRepository extends JpaRepository<WhatsappMensagem, String> {
    long countByEnviadoEmGreaterThanEqualAndEnviadoEmLessThan(OffsetDateTime de, OffsetDateTime ate);

    Optional<WhatsappMensagem> findByMensagemGateway(String mensagemGateway);

    boolean existsByIdempotencyKey(String chave);

    // Quantas mensagens a campanha já enviou a partir de um instante (limite diário).
    @Query("select count(m) from WhatsappMensagem m where m.enviadoEm>=:desde and m.idDestinatario in (select d.id from CampanhaDestinatario d where d.idCampanha=:campanha)")
    long enviadasDaCampanhaDesde(@Param("campanha") String campanha, @Param("desde") OffsetDateTime desde);

    List<WhatsappMensagem> findAllByIdDestinatarioIn(List<String> ids);
}

package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.CampanhaDestinatario;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CampanhaDestinatarioRepository extends JpaRepository<CampanhaDestinatario, String> {
    List<CampanhaDestinatario> findAllByIdCampanha(String idCampanha);

    // Quem ainda está recebendo mensagens (de qualquer campanha): não pode entrar em outra campanha ao mesmo tempo.
    @Query("select d.idCliente from CampanhaDestinatario d where d.status='ATIVO'")
    List<String> clientesEmAndamento();

    // Fila: destinatários ativos, já na hora da próxima mensagem, de campanhas aprovadas; os mais antigos primeiro.
    @Query("select d from CampanhaDestinatario d where d.status='ATIVO' and d.proximaEtapaEm<=:agora and d.idCampanha in (select c.id from CampanhaReativacao c where c.status='APROVADA') order by d.proximaEtapaEm")
    List<CampanhaDestinatario> fila(@Param("agora") OffsetDateTime agora);

    // Quem recebeu mensagem há pouco: candidatos a ser o autor de uma resposta recebida.
    List<CampanhaDestinatario> findAllByUltimoEnvioEmAfter(OffsetDateTime desde);

    @Query("select d from CampanhaDestinatario d where d.idCliente=:cliente and d.status in ('ATIVO','CONCLUIDO','RESPONDEU')")
    List<CampanhaDestinatario> ativosDoCliente(@Param("cliente") String cliente);
}

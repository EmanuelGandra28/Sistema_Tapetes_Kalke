package br.com.tapeteskalke.repository;

import br.com.tapeteskalke.entity.UsuarioVisto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioVistoRepository extends JpaRepository<UsuarioVisto, String> {
    List<UsuarioVisto> findAllByIdUsuario(String idUsuario);

    boolean existsByIdUsuarioAndChave(String idUsuario, String chave);

    List<UsuarioVisto> findAllByIdUsuarioAndChaveStartingWith(String idUsuario, String prefixo);
}

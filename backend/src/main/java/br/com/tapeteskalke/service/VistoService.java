package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.UsuarioVisto;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.UsuarioVistoRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Marca o que o usuário logado já viu (tutorial de cada tela, novidades de cada versão). Cada um só enxerga e altera o que é seu.
@Service
public class VistoService {
    private static final Pattern CHAVE = Pattern.compile("^[a-z0-9]+:[a-z0-9._-]{1,50}$");
    private static final Pattern PREFIXO = Pattern.compile("^[a-z0-9]+:$");

    private final UsuarioVistoRepository repo;
    private final AuditService audit;

    public VistoService(UsuarioVistoRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<String> listar() {
        return repo.findAllByIdUsuario(audit.userId()).stream().map(UsuarioVisto::getChave).sorted().toList();
    }

    /** Marca como visto; repetir não faz nada (dois cliques ou duas abas não geram erro). */
    @Transactional
    public void marcar(String chave) {
        if (chave == null || !CHAVE.matcher(chave).matches()) throw new BusinessException("CHAVE_INVALIDA", "Chave inválida", HttpStatus.BAD_REQUEST);
        var usuario = audit.userId();
        if (repo.existsByIdUsuarioAndChave(usuario, chave)) return;
        var v = new UsuarioVisto();
        v.setIdUsuario(usuario);
        v.setChave(chave);
        audit.created(v);
        repo.save(v);
    }

    /** "Rever tutoriais": apaga as marcas de um grupo (prefixo como "ajuda:") só do usuário logado. */
    @Transactional
    public void limpar(String prefixo) {
        if (prefixo == null || !PREFIXO.matcher(prefixo).matches()) throw new BusinessException("PREFIXO_INVALIDO", "Prefixo inválido", HttpStatus.BAD_REQUEST);
        repo.deleteAll(repo.findAllByIdUsuarioAndChaveStartingWith(audit.userId(), prefixo));
    }
}

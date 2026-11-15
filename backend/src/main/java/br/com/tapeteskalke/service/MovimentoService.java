package br.com.tapeteskalke.service;

import br.com.tapeteskalke.dto.OperacaoDtos.MovimentoResponse;
import br.com.tapeteskalke.entity.MovimentoEstoque;
import br.com.tapeteskalke.entity.Usuario;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.MovimentoEstoqueRepository;
import br.com.tapeteskalke.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Consulta das movimentações de estoque com filtros opcionais (tela e exportação usam a mesma regra).
@Service
public class MovimentoService {
    public record Filtro(String produtoId, String saldo, String tipo, LocalDate inicio, LocalDate fim) {}

    private final MovimentoEstoqueRepository repo;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public MovimentoService(MovimentoEstoqueRepository repo, UsuarioRepository usuarios, Clock clock) {
        this.repo = repo;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<MovimentoResponse> buscar(Filtro f, int page, int size) {
        var pagina = repo.findAll(spec(f), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "dataMovimento")));
        var nomes = nomesDosUsuarios(pagina.getContent());
        return pagina.map(m -> response(m, nomes));
    }

    @Transactional(readOnly = true)
    public List<MovimentoResponse> todos(Filtro f) {
        var lista = repo.findAll(spec(f), Sort.by(Sort.Direction.DESC, "dataMovimento"));
        var nomes = nomesDosUsuarios(lista);
        return lista.stream().map(m -> response(m, nomes)).toList();
    }

    private Specification<MovimentoEstoque> spec(Filtro f) {
        if (f.inicio() != null && f.fim() != null && f.inicio().isAfter(f.fim()))
            throw new BusinessException("PERIODO_INVALIDO", "A data inicial deve ser anterior ou igual à data final", HttpStatus.BAD_REQUEST);
        var zona = clock.getZone();
        return (root, query, cb) -> {
            var filtros = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (f.produtoId() != null && !f.produtoId().isBlank()) filtros.add(cb.equal(root.get("produto").get("id"), f.produtoId()));
            if (f.saldo() != null && !f.saldo().isBlank()) filtros.add(cb.equal(root.get("saldo"), f.saldo()));
            if (f.tipo() != null && !f.tipo().isBlank()) filtros.add(cb.equal(root.get("tipoMovimento"), f.tipo()));
            if (f.inicio() != null) filtros.add(cb.greaterThanOrEqualTo(root.get("dataMovimento"), f.inicio().atStartOfDay(zona).toOffsetDateTime()));
            if (f.fim() != null) filtros.add(cb.lessThan(root.get("dataMovimento"), f.fim().plusDays(1).atStartOfDay(zona).toOffsetDateTime()));
            return cb.and(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Map<String, String> nomesDosUsuarios(List<MovimentoEstoque> lista) {
        var ids = lista.stream().map(MovimentoEstoque::getIdUsuario).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : usuarios.findAllById(ids).stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNome, (a, b) -> a));
    }

    private MovimentoResponse response(MovimentoEstoque m, Map<String, String> nomes) {
        var p = m.getProduto();
        return new MovimentoResponse(m.getId(), m.getDataMovimento(), p.getId(), p.getNome(), m.getSaldo(), m.getTipoMovimento(), m.getOrigemMovimento(),
                m.getIdOrigem(), m.getQuantidade(), m.getEstoqueAnterior(), m.getEstoquePosterior(),
                m.getIdUsuario() == null ? null : nomes.getOrDefault(m.getIdUsuario(), null), m.getObservacao());
    }

}

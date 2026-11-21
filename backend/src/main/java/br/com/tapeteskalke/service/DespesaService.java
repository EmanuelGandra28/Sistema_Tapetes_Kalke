package br.com.tapeteskalke.service;

import br.com.tapeteskalke.dto.OperacaoDtos.DespesaRequest;
import br.com.tapeteskalke.entity.Despesa;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.exception.ResourceNotFoundException;
import br.com.tapeteskalke.repository.DespesaRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespesaService {
    private final DespesaRepository repo;
    private final AuditService audit;
    private final Clock clock;

    public DespesaService(DespesaRepository repo, AuditService audit, Clock clock) {
        this.repo = repo;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Despesa create(DespesaRequest r) {
        var d = new Despesa();
        aplicar(d, r);
        audit.created(d);
        return repo.save(d);
    }

    @Transactional
    public Despesa update(String id, DespesaRequest r) {
        var d = ativa(id);
        aplicar(d, r);
        audit.changed(d);
        return repo.save(d);
    }

    // Exclusão lógica: some das listas e dos relatórios, mas fica registrada com quem excluiu e por quê.
    @Transactional
    public void delete(String id, String motivo) {
        if (motivo == null || motivo.isBlank())
            throw new BusinessException("MOTIVO_OBRIGATORIO", "Informe o motivo da exclusão", HttpStatus.BAD_REQUEST);
        var d = ativa(id);
        d.setExcluidaEm(OffsetDateTime.now(clock));
        d.setExcluidaPor(audit.userId());
        d.setMotivoExclusao(motivo.trim().length() > 255 ? motivo.trim().substring(0, 255) : motivo.trim());
        audit.changed(d);
        repo.save(d);
    }

    @Transactional(readOnly = true)
    public List<Despesa> list() {
        return repo.findAllByExcluidaEmIsNullOrderByDataDespesaDesc();
    }

    private Despesa ativa(String id) {
        var d = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Despesa", id));
        if (d.getExcluidaEm() != null) throw new ResourceNotFoundException("Despesa", id);
        return d;
    }

    private void aplicar(Despesa d, DespesaRequest r) {
        d.setTipo(r.tipo());
        d.setDescricao(r.descricao());
        d.setValor(r.valor());
        d.setDataDespesa(r.dataDespesa());
        d.setObservacoes(r.observacoes());
    }
}

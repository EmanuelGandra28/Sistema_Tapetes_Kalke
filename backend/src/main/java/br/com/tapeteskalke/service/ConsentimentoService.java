package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.ClienteConsentimento;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.exception.ResourceNotFoundException;
import br.com.tapeteskalke.repository.ClienteConsentimentoRepository;
import br.com.tapeteskalke.repository.ClienteRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Consentimento LGPD para campanhas de WhatsApp. A evidência (como o cliente autorizou) é escrita por uma pessoa: o sistema nunca a inventa.
@Service
public class ConsentimentoService {
    public static final String FINALIDADE = "marketing_reativacao";

    private final ClienteConsentimentoRepository repo;
    private final ClienteRepository clientes;
    private final AuditService audit;
    private final Clock clock;

    public ConsentimentoService(ClienteConsentimentoRepository repo, ClienteRepository clientes, AuditService audit, Clock clock) {
        this.repo = repo;
        this.clientes = clientes;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void registrar(String clienteId, String evidencia) {
        var cliente = clientes.findById(clienteId).orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));
        var texto = evidencia == null ? "" : evidencia.strip();
        if (texto.length() < 10 || texto.length() > 2000)
            throw new BusinessException("EVIDENCIA_INVALIDA", "Descreva como o cliente autorizou (de 10 a 2000 caracteres). Ex.: \"Autorizou no balcão em 21/11/2026, falando com a vendedora Ana\"", HttpStatus.BAD_REQUEST);
        if (WhatsappUtil.telefoneE164(cliente.getTelefone()) == null)
            throw new BusinessException("TELEFONE_INVALIDO", "O cliente precisa ter um telefone válido (com DDD) para receber mensagens", HttpStatus.BAD_REQUEST);
        if (repo.findByIdClienteAndFinalidadeAndRevogadoEmIsNull(clienteId, FINALIDADE).isPresent())
            throw new BusinessException("CONSENTIMENTO_JA_REGISTRADO", "Este cliente já tem a autorização registrada", HttpStatus.CONFLICT);
        var c = new ClienteConsentimento();
        c.setIdCliente(clienteId);
        c.setFinalidade(FINALIDADE);
        c.setEvidencia(texto);
        c.setOrigem("TELA");
        c.setConcedidoEm(OffsetDateTime.now(clock));
        audit.created(c);
        repo.save(c);
    }

    @Transactional
    public void revogar(String clienteId, String motivo) {
        var c = repo.findByIdClienteAndFinalidadeAndRevogadoEmIsNull(clienteId, FINALIDADE).orElseThrow(() -> new ResourceNotFoundException("Consentimento", clienteId));
        revogar(c, motivo);
    }

    private void revogar(ClienteConsentimento c, String motivo) {
        c.setRevogadoEm(OffsetDateTime.now(clock));
        c.setMotivoRevogacao(motivo == null ? null : motivo.length() > 120 ? motivo.substring(0, 120) : motivo);
        repo.save(c);
    }

    /** Descadastro vindo do cliente pelo WhatsApp (SAIR): revoga o consentimento de quem tem esse telefone. Devolve quantos foram revogados. */
    @Transactional
    public int revogarPorTelefone(String telefoneE164, String motivo) {
        int total = 0;
        for (var c : repo.findAllByFinalidadeAndRevogadoEmIsNull(FINALIDADE)) {
            var cliente = clientes.findById(c.getIdCliente()).orElse(null);
            if (cliente != null && Objects.equals(WhatsappUtil.telefoneE164(cliente.getTelefone()), telefoneE164)) { revogar(c, motivo); total++; }
        }
        return total;
    }

    @Transactional(readOnly = true)
    public Optional<ClienteConsentimento> vigente(String clienteId) {
        return repo.findByIdClienteAndFinalidadeAndRevogadoEmIsNull(clienteId, FINALIDADE);
    }

    /** Ids dos clientes com autorização vigente (para mostrar a situação na lista de clientes). */
    @Transactional(readOnly = true)
    public Set<String> idsComConsentimento() {
        return repo.findAllByFinalidadeAndRevogadoEmIsNull(FINALIDADE).stream().map(ClienteConsentimento::getIdCliente).collect(Collectors.toSet());
    }
}

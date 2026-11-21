package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.*;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.exception.ResourceNotFoundException;
import br.com.tapeteskalke.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Reativação de clientes: escolhe quem está há 2 meses ou mais sem comprar (e autorizou receber mensagens), monta campanhas de até 3
// mensagens com aprovação manual e dispara a fila respeitando horário comercial, limite diário e a cota mensal do plano.
@Service
public class ReativacaoService {
    public static final int ETAPAS = 3;
    public static final int MESES_SEM_COMPRAR = 2;
    public static final int HORA_INICIO = 8, HORA_FIM = 20; // janela do gateway: 08h às 20h
    private static final double BLOQUEIO_DA_COTA = 0.9;
    private static final String LOJA = "Tapetes Kalke";

    public record Candidato(String id, String nome, String telefone, LocalDate ultimaCompra, long diasSemComprar, BigDecimal totalCompras, String valor, String ciclo) {}
    public record Processamento(int enviadas, int falhas, int puladas, String motivoParada) {}
    public record CampanhaResponse(String id, String nome, String status, int intervaloDias, int limiteDiario, OffsetDateTime criadaEm, OffsetDateTime aprovadaEm,
                                   long destinatarios, long enviadas) {}
    public record CriarResultado(CampanhaResponse campanha, int incluidos, int ignorados) {}
    public record DestinatarioResponse(String id, String cliente, String telefone, String status, int etapa, String resposta, String respostaTexto, OffsetDateTime respostaEm, boolean recomprou) {}
    public record Resultados(CampanhaResponse campanha, long enviadas, long responderam, long interessados, long semInteresse, long pediramSair, long recompraram, List<DestinatarioResponse> destinatarios) {}
    public record Status(boolean configurado, boolean dentroDoHorario, long enviadasNoMes, int cotaMensal, int bloqueioNaCota, boolean webhookConfigurado) {}

    private final CampanhaReativacaoRepository campanhas;
    private final CampanhaDestinatarioRepository destinatarios;
    private final WhatsappMensagemRepository mensagens;
    private final ClienteRepository clientes;
    private final VendaRepository vendas;
    private final ConsentimentoService consentimentos;
    private final MensagemService modelos;
    private final WhatsAppProvider provider;
    private final AuditService audit;
    private final Clock clock;
    private final int cotaMensal;
    private final boolean webhookConfigurado;

    public ReativacaoService(CampanhaReativacaoRepository campanhas, CampanhaDestinatarioRepository destinatarios, WhatsappMensagemRepository mensagens, ClienteRepository clientes,
                             VendaRepository vendas, ConsentimentoService consentimentos, MensagemService modelos, WhatsAppProvider provider, AuditService audit, Clock clock,
                             @Value("${app.whatsapp.cota-mensal:500}") int cotaMensal, @Value("${app.whatsapp.webhook-secret:}") String webhookSecret) {
        this.campanhas = campanhas;
        this.destinatarios = destinatarios;
        this.mensagens = mensagens;
        this.clientes = clientes;
        this.vendas = vendas;
        this.consentimentos = consentimentos;
        this.modelos = modelos;
        this.provider = provider;
        this.audit = audit;
        this.clock = clock;
        this.cotaMensal = cotaMensal;
        this.webhookConfigurado = webhookSecret != null && !webhookSecret.isBlank();
    }

    // ---------- regras puras ----------

    /** Faixa de valor pela posição no ranking de quanto o cliente já comprou: 20% de cima = OURO, próximos 30% = PRATA, resto = BRONZE. */
    public static String faixaValor(int posicao, int total) {
        if (posicao < Math.ceil(total * 0.2)) return "OURO";
        return posicao < Math.ceil(total * 0.5) ? "PRATA" : "BRONZE";
    }

    /** Ciclo de recompra: quanto tempo o cliente está sem comprar. */
    public static String ciclo(long dias) {
        return dias >= 180 ? "INATIVO_LONGO" : dias >= 90 ? "INATIVO" : "EM_RISCO";
    }

    public static boolean dentroDoHorario(ZonedDateTime agora) {
        return agora.getHour() >= HORA_INICIO && agora.getHour() < HORA_FIM;
    }

    // ---------- candidatos e campanhas ----------

    private boolean semComprar(Cliente c, LocalDate hoje) {
        return c.getUltimaCompra() != null && ChronoUnit.MONTHS.between(c.getUltimaCompra().withDayOfMonth(1), hoje.withDayOfMonth(1)) >= MESES_SEM_COMPRAR;
    }

    /** Quem pode entrar numa campanha: ativo, 2+ meses sem comprar, telefone válido, autorização vigente e fora de outra campanha em andamento. */
    @Transactional(readOnly = true)
    public List<Candidato> candidatos() {
        var hoje = LocalDate.now(clock);
        var comConsentimento = consentimentos.idsComConsentimento();
        var emAndamento = new HashSet<>(destinatarios.clientesEmAndamento());
        var elegiveis = clientes.findAll().stream()
                .filter(c -> c.isAtivo() && "ATIVO".equals(c.getStatusCadastral()) && semComprar(c, hoje) && comConsentimento.contains(c.getId())
                        && !emAndamento.contains(c.getId()) && WhatsappUtil.telefoneE164(c.getTelefone()) != null)
                .sorted(Comparator.comparing((Cliente c) -> c.getTotalCompras() == null ? BigDecimal.ZERO : c.getTotalCompras()).reversed()).toList();
        var lista = new ArrayList<Candidato>();
        for (int i = 0; i < elegiveis.size(); i++) {
            var c = elegiveis.get(i);
            long dias = ChronoUnit.DAYS.between(c.getUltimaCompra(), hoje);
            lista.add(new Candidato(c.getId(), c.getNome(), c.getTelefone(), c.getUltimaCompra(), dias, c.getTotalCompras() == null ? BigDecimal.ZERO : c.getTotalCompras(), faixaValor(i, elegiveis.size()), ciclo(dias)));
        }
        return lista;
    }

    @Transactional
    public CriarResultado criar(String nome, List<String> clienteIds, int intervaloDias, int limiteDiario) {
        if (nome == null || nome.isBlank()) throw new BusinessException("NOME_OBRIGATORIO", "Dê um nome para a campanha", HttpStatus.BAD_REQUEST);
        var permitidos = candidatos().stream().map(Candidato::id).collect(Collectors.toSet());
        var escolhidos = clienteIds.stream().distinct().filter(permitidos::contains).toList();
        if (escolhidos.isEmpty())
            throw new BusinessException("SEM_DESTINATARIOS", "Nenhum dos clientes escolhidos pode receber a campanha (precisam estar 2+ meses sem comprar, com telefone válido e autorização registrada)", HttpStatus.BAD_REQUEST);
        var c = new CampanhaReativacao();
        c.setNome(nome.strip());
        c.setIntervaloDias(Math.min(Math.max(intervaloDias, 1), 60));
        c.setLimiteDiario(Math.min(Math.max(limiteDiario, 1), 200));
        audit.created(c);
        c = campanhas.save(c);
        var agora = OffsetDateTime.now(clock);
        for (var id : escolhidos) {
            var d = new CampanhaDestinatario();
            d.setIdCampanha(c.getId());
            d.setIdCliente(id);
            d.setProximaEtapaEm(agora);
            audit.created(d);
            destinatarios.save(d);
        }
        return new CriarResultado(resposta(c), escolhidos.size(), clienteIds.size() - escolhidos.size());
    }

    @Transactional(readOnly = true)
    public List<CampanhaResponse> listar() {
        return campanhas.findAllByOrderByCreatedAtDesc().stream().map(this::resposta).toList();
    }

    private CampanhaResponse resposta(CampanhaReativacao c) {
        var dest = destinatarios.findAllByIdCampanha(c.getId());
        long enviadas = dest.isEmpty() ? 0 : mensagens.findAllByIdDestinatarioIn(dest.stream().map(CampanhaDestinatario::getId).toList()).size();
        return new CampanhaResponse(c.getId(), c.getNome(), c.getStatus(), c.getIntervaloDias(), c.getLimiteDiario(), c.getCreatedAt(), c.getAprovadaEm(), dest.size(), enviadas);
    }

    private CampanhaReativacao campanha(String id) {
        return campanhas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Campanha", id));
    }

    /** Aprovação manual: nada é enviado antes de uma pessoa aprovar. */
    @Transactional
    public CampanhaResponse aprovar(String id) {
        var c = campanha(id);
        if (!"RASCUNHO".equals(c.getStatus()) && !"PAUSADA".equals(c.getStatus()))
            throw new BusinessException("STATUS_INVALIDO", "Só uma campanha em rascunho ou pausada pode ser aprovada", HttpStatus.CONFLICT);
        c.setStatus("APROVADA");
        c.setAprovadaPor(audit.userId());
        c.setAprovadaEm(OffsetDateTime.now(clock));
        audit.changed(c);
        return resposta(campanhas.save(c));
    }

    @Transactional
    public CampanhaResponse pausar(String id) {
        var c = campanha(id);
        if (!"APROVADA".equals(c.getStatus())) throw new BusinessException("STATUS_INVALIDO", "Só uma campanha aprovada pode ser pausada", HttpStatus.CONFLICT);
        c.setStatus("PAUSADA");
        audit.changed(c);
        return resposta(campanhas.save(c));
    }

    @Transactional
    public CampanhaResponse cancelar(String id) {
        var c = campanha(id);
        if ("CONCLUIDA".equals(c.getStatus()) || "CANCELADA".equals(c.getStatus())) throw new BusinessException("STATUS_INVALIDO", "Esta campanha já terminou", HttpStatus.CONFLICT);
        c.setStatus("CANCELADA");
        audit.changed(c);
        for (var d : destinatarios.findAllByIdCampanha(id)) if ("ATIVO".equals(d.getStatus())) { d.setStatus("CANCELADO"); destinatarios.save(d); }
        return resposta(campanhas.save(c));
    }

    @Transactional(readOnly = true)
    public Status status() {
        var agora = ZonedDateTime.now(clock);
        var inicio = agora.toLocalDate().withDayOfMonth(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
        long mes = mensagens.countByEnviadoEmGreaterThanEqualAndEnviadoEmLessThan(inicio, inicio.plusMonths(1));
        return new Status(provider.configurado(), dentroDoHorario(agora), mes, cotaMensal, (int) (cotaMensal * BLOQUEIO_DA_COTA), webhookConfigurado);
    }

    // ---------- fila de envio ----------

    /** Envia o que está na vez. Chamado de tempos em tempos por uma rotina externa (o servidor grátis dorme, então ele não agenda sozinho). */
    public Processamento processarFila() {
        if (!provider.configurado()) return new Processamento(0, 0, 0, "O gateway de WhatsApp não está configurado no servidor");
        var agora = OffsetDateTime.now(clock);
        var local = agora.atZoneSameInstant(clock.getZone());
        if (!dentroDoHorario(local)) return new Processamento(0, 0, 0, "Fora do horário comercial (08h às 20h)");
        var inicioDia = local.toLocalDate().atStartOfDay(clock.getZone()).toOffsetDateTime();
        var inicioMes = local.toLocalDate().withDayOfMonth(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
        long usadasNoMes = mensagens.countByEnviadoEmGreaterThanEqualAndEnviadoEmLessThan(inicioMes, inicioMes.plusMonths(1));
        long teto = (long) (cotaMensal * BLOQUEIO_DA_COTA);
        var cacheCampanha = new HashMap<String, CampanhaReativacao>();
        var hoje = new HashMap<String, Long>();
        var tocadas = new HashSet<String>();
        int enviadas = 0, falhas = 0, puladas = 0;
        String parada = null;
        for (var d : destinatarios.fila(agora)) {
            if (usadasNoMes + enviadas >= teto) { parada = "Cota mensal quase no fim (" + teto + " de " + cotaMensal + " mensagens)"; break; }
            var campanha = cacheCampanha.computeIfAbsent(d.getIdCampanha(), this::campanha);
            if (hoje.computeIfAbsent(campanha.getId(), k -> mensagens.enviadasDaCampanhaDesde(k, inicioDia)) >= campanha.getLimiteDiario()) { puladas++; continue; }
            tocadas.add(campanha.getId());
            try {
                var r = enviarEtapa(d, campanha, agora);
                if (r == 1) { enviadas++; hoje.merge(campanha.getId(), 1L, Long::sum); } else if (r == -1) falhas++; else puladas++;
            } catch (GatewayException e) {
                if (e.deveParar()) { parada = e.getMessage() + (e.requisicaoId() == null ? "" : " (requisição " + e.requisicaoId() + ")"); break; }
                if (e.definitivo()) { falhar(d, e.codigo()); falhas++; } else puladas++;
            }
        }
        for (var id : tocadas) concluirSeAcabou(id);
        return new Processamento(enviadas, falhas, puladas, parada);
    }

    // 1 = enviada, -1 = falhou de vez, 0 = pulada (tenta de novo depois)
    private int enviarEtapa(CampanhaDestinatario d, CampanhaReativacao campanha, OffsetDateTime agora) {
        var cliente = clientes.findById(d.getIdCliente()).orElse(null);
        if (cliente == null || !cliente.isAtivo()) { fechar(d, "CANCELADO"); return 0; }
        // Se o cliente já voltou a comprar depois que a campanha foi criada, não faz sentido continuar.
        if (cliente.getUltimaCompra() != null && !cliente.getUltimaCompra().isBefore(d.getCreatedAt().atZoneSameInstant(clock.getZone()).toLocalDate())) { fechar(d, "RECOMPROU"); return 0; }
        var consentimento = consentimentos.vigente(cliente.getId()).orElse(null);
        if (consentimento == null) { fechar(d, "CANCELADO"); return 0; }
        var telefone = WhatsappUtil.telefoneE164(cliente.getTelefone());
        if (telefone == null) { falhar(d, "TELEFONE_INVALIDO"); return -1; }
        int etapa = d.getEtapaAtual() + 1;
        var chave = "reativacao-" + d.getId() + "-" + etapa;
        if (mensagens.existsByIdempotencyKey(chave)) { avancar(d, campanha, etapa, agora); return 0; } // já foi enviada; só alcança o estado
        var texto = modelos.renderizar("REATIVACAO_" + etapa, dadosDaMensagem(cliente, agora.atZoneSameInstant(clock.getZone()).toLocalDate()));
        if (texto.isEmpty()) return 0; // mensagem desativada na aba Mensagens: espera
        var enviada = provider.enviarTexto(telefone, texto.get(), ConsentimentoService.FINALIDADE, chave, consentimento.getEvidencia());
        var m = new WhatsappMensagem();
        m.setIdDestinatario(d.getId());
        m.setTelefone(telefone);
        m.setEtapa(etapa);
        m.setIdempotencyKey(chave);
        m.setMensagemGateway(enviada.mensagemId());
        m.setSituacao(enviada.situacao());
        m.setEnviadoEm(agora);
        audit.created(m);
        mensagens.save(m);
        avancar(d, campanha, etapa, agora);
        return 1;
    }

    private Map<String, String> dadosDaMensagem(Cliente c, LocalDate hoje) {
        var primeiro = c.getNome().strip().split("\\s+")[0];
        var ultimo = vendas.historyByClient(c.getId()).stream().filter(v -> "FINALIZADA".equals(v.getStatus())).findFirst()
                .flatMap(v -> v.getItens().stream().findFirst()).map(VendaItem::getProdutoNomeSnapshot).orElse("seu último tapete");
        return Map.of("nome", primeiro, "loja", LOJA, "dias_sem_comprar", String.valueOf(ChronoUnit.DAYS.between(c.getUltimaCompra(), hoje)), "ultimo_produto", ultimo);
    }

    private void avancar(CampanhaDestinatario d, CampanhaReativacao campanha, int etapa, OffsetDateTime agora) {
        d.setEtapaAtual(etapa);
        d.setUltimoEnvioEm(agora);
        if (d.getPrimeiroEnvioEm() == null) d.setPrimeiroEnvioEm(agora);
        if (etapa >= ETAPAS) d.setStatus("CONCLUIDO"); else d.setProximaEtapaEm(agora.plusDays(campanha.getIntervaloDias()));
        destinatarios.save(d);
    }

    private void fechar(CampanhaDestinatario d, String status) {
        d.setStatus(status);
        destinatarios.save(d);
    }

    private void falhar(CampanhaDestinatario d, String codigo) {
        fechar(d, "FALHOU");
        var m = new WhatsappMensagem();
        m.setIdDestinatario(d.getId());
        m.setTelefone("-");
        m.setEtapa(d.getEtapaAtual() + 1);
        m.setIdempotencyKey("falha-" + d.getId() + "-" + (d.getEtapaAtual() + 1));
        m.setSituacao("falhou");
        m.setErroCodigo(codigo);
        m.setEnviadoEm(OffsetDateTime.now(clock));
        if (!mensagens.existsByIdempotencyKey(m.getIdempotencyKey())) { audit.created(m); mensagens.save(m); }
    }

    private void concluirSeAcabou(String campanhaId) {
        var c = campanha(campanhaId);
        if ("APROVADA".equals(c.getStatus()) && destinatarios.findAllByIdCampanha(campanhaId).stream().noneMatch(x -> "ATIVO".equals(x.getStatus()))) {
            c.setStatus("CONCLUIDA");
            campanhas.save(c);
        }
    }

    // ---------- respostas e resultados ----------

    /** Resposta de um cliente recebida pelo WhatsApp: liga à campanha mais recente, classifica e interrompe novas mensagens para ele. */
    @Transactional
    public boolean registrarResposta(String remetenteE164, String texto, OffsetDateTime recebidoEm) {
        var recentes = destinatarios.findAllByUltimoEnvioEmAfter(OffsetDateTime.now(clock).minusDays(60)).stream()
                .sorted(Comparator.comparing(CampanhaDestinatario::getUltimoEnvioEm).reversed()).toList();
        for (var d : recentes) {
            var cliente = clientes.findById(d.getIdCliente()).orElse(null);
            if (cliente == null || !Objects.equals(WhatsappUtil.telefoneE164(cliente.getTelefone()), remetenteE164)) continue;
            var classe = WhatsappUtil.classificar(texto);
            d.setRespostaClasse(classe.name());
            d.setRespostaTexto(texto == null ? null : texto.length() > 500 ? texto.substring(0, 500) : texto);
            d.setRespostaEm(recebidoEm == null ? OffsetDateTime.now(clock) : recebidoEm);
            if (classe == WhatsappUtil.Resposta.PEDIU_SAIR) { d.setStatus("SAIU"); consentimentos.revogarPorTelefone(remetenteE164, "Pediu para sair pelo WhatsApp"); }
            else if ("ATIVO".equals(d.getStatus()) || "CONCLUIDO".equals(d.getStatus())) d.setStatus("RESPONDEU");
            destinatarios.save(d);
            return true;
        }
        return false;
    }

    /** O cliente pediu para sair (evento do gateway): interrompe as campanhas dele e revoga a autorização. */
    @Transactional
    public void registrarDescadastro(String telefoneE164) {
        consentimentos.revogarPorTelefone(telefoneE164, "Pediu para sair pelo WhatsApp");
        for (var d : destinatarios.findAllByUltimoEnvioEmAfter(OffsetDateTime.now(clock).minusDays(365))) {
            var cliente = clientes.findById(d.getIdCliente()).orElse(null);
            if (cliente != null && Objects.equals(WhatsappUtil.telefoneE164(cliente.getTelefone()), telefoneE164) && ("ATIVO".equals(d.getStatus()) || "CONCLUIDO".equals(d.getStatus()))) { d.setStatus("SAIU"); destinatarios.save(d); }
        }
    }

    @Transactional
    public void atualizarSituacao(String mensagemGateway, String situacao) {
        mensagens.findByMensagemGateway(mensagemGateway).ifPresent(m -> { m.setSituacao(situacao); mensagens.save(m); });
    }

    @Transactional(readOnly = true)
    public Resultados resultados(String campanhaId) {
        var c = campanha(campanhaId);
        var dest = destinatarios.findAllByIdCampanha(campanhaId);
        var porId = clientes.findAllById(dest.stream().map(CampanhaDestinatario::getIdCliente).toList()).stream().collect(Collectors.toMap(Cliente::getId, x -> x));
        var linhas = new ArrayList<DestinatarioResponse>();
        long recompraram = 0, interessados = 0, semInteresse = 0, sair = 0, responderam = 0;
        for (var d : dest) {
            var cli = porId.get(d.getIdCliente());
            boolean voltou = "RECOMPROU".equals(d.getStatus()) || (cli != null && d.getPrimeiroEnvioEm() != null && cli.getUltimaCompra() != null
                    && !cli.getUltimaCompra().isBefore(d.getPrimeiroEnvioEm().atZoneSameInstant(clock.getZone()).toLocalDate())
                    && !cli.getUltimaCompra().isAfter(d.getPrimeiroEnvioEm().atZoneSameInstant(clock.getZone()).toLocalDate().plusDays(30)));
            if (voltou) recompraram++;
            if (d.getRespostaClasse() != null) {
                responderam++;
                switch (d.getRespostaClasse()) { case "INTERESSADO" -> interessados++; case "SEM_INTERESSE" -> semInteresse++; case "PEDIU_SAIR" -> sair++; default -> { } }
            }
            linhas.add(new DestinatarioResponse(d.getId(), cli == null ? "?" : cli.getNome(), cli == null ? "" : cli.getTelefone(), d.getStatus(), d.getEtapaAtual(), d.getRespostaClasse(), d.getRespostaTexto(), d.getRespostaEm(), voltou));
        }
        long enviadas = dest.isEmpty() ? 0 : mensagens.findAllByIdDestinatarioIn(dest.stream().map(CampanhaDestinatario::getId).toList()).stream().filter(m -> m.getMensagemGateway() != null).count();
        return new Resultados(resposta(c), enviadas, responderam, interessados, semInteresse, sair, recompraram, linhas);
    }
}

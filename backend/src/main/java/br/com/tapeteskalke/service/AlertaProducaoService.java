package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.AlertaProducao;
import br.com.tapeteskalke.entity.ConfiguracaoLoja;
import br.com.tapeteskalke.entity.Produto;
import br.com.tapeteskalke.entity.WhatsappMensagem;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Avisa a dona pelo WhatsApp quando o sistema abre uma ordem de produção automática: uma única mensagem com a lista dos tapetes
// (e até 3 fotos). Cada ordem entra numa fila; a rotina agrupa tudo o que está pendente, só envia entre 8h e 20h e nunca repete.
// Uma falha aqui NUNCA afeta a venda nem a criação da ordem.
@Service
public class AlertaProducaoService {
    private static final Logger log = LoggerFactory.getLogger(AlertaProducaoService.class);
    public static final String FINALIDADE = "aviso_operacional";
    public static final int FOTOS_MAXIMAS = 3;
    public static final int TAPETES_NA_LISTA = 10;
    private static final int TENTATIVAS_MAXIMAS = 5;
    private static final String LOJA = "Tapetes Kalke";

    public record Config(String whatsapp, boolean ativo, long pendentes, long falhas, boolean gatewayConfigurado) {}
    public record Processamento(int alertas, int mensagens, String motivo) {}

    private final AlertaProducaoRepository alertas;
    private final ConfiguracaoLojaRepository configuracao;
    private final OrdemProducaoRepository ordens;
    private final ProdutoRepository produtos;
    private final ProdutoImagemRepository imagens;
    private final WhatsappMensagemRepository mensagens;
    private final MensagemService modelos;
    private final WhatsAppProvider provider;
    private final UsuarioRepository usuarios;
    private final AuditService audit;
    private final Clock clock;
    private final int cotaMensal;
    private final String linkOrdens;

    public AlertaProducaoService(AlertaProducaoRepository alertas, ConfiguracaoLojaRepository configuracao, OrdemProducaoRepository ordens, ProdutoRepository produtos,
                                 ProdutoImagemRepository imagens, WhatsappMensagemRepository mensagens, MensagemService modelos, WhatsAppProvider provider, UsuarioRepository usuarios,
                                 AuditService audit, Clock clock, @Value("${app.whatsapp.cota-mensal:500}") int cotaMensal,
                                 @Value("${app.cors.allowed-origin}") String origem, @Value("${app.frontend-path:/Sistema_Tapetes_Kalke}") String caminho) {
        this.alertas = alertas;
        this.configuracao = configuracao;
        this.ordens = ordens;
        this.produtos = produtos;
        this.imagens = imagens;
        this.mensagens = mensagens;
        this.modelos = modelos;
        this.provider = provider;
        this.usuarios = usuarios;
        this.audit = audit;
        this.clock = clock;
        this.cotaMensal = cotaMensal;
        this.linkOrdens = origem.replaceAll("/+$", "") + caminho + "/ordens-producao";
    }

    // ---------- regras puras ----------

    private static String numero(BigDecimal v) { return v.stripTrailingZeros().toPlainString(); }

    /** Uma linha da lista: "1) Tapete Floral (Azul, 2x3) — saldo 2, mínimo 5 → produzir 8". */
    public static String linha(int posicao, String nome, String cor, String tamanho, BigDecimal saldo, BigDecimal minimo, BigDecimal quantidade) {
        var detalhes = new ArrayList<String>();
        if (cor != null && !cor.isBlank()) detalhes.add(cor.strip());
        if (tamanho != null && !tamanho.isBlank()) detalhes.add(tamanho.strip());
        return posicao + ") " + nome + (detalhes.isEmpty() ? "" : " (" + String.join(", ", detalhes) + ")") + " — saldo " + numero(saldo) + ", mínimo " + numero(minimo) + " → produzir " + numero(quantidade);
    }

    /** Lista numerada; passando de {@code limite} tapetes, mostra os primeiros e avisa quantos ficaram de fora. */
    public static String montarLista(List<String> linhas, int limite) {
        var mostrar = linhas.size() > limite ? linhas.subList(0, limite) : linhas;
        var texto = String.join("\n", mostrar);
        return linhas.size() > limite ? texto + "\n...e mais " + (linhas.size() - limite) + (linhas.size() - limite == 1 ? " tapete" : " tapetes") : texto;
    }

    // ---------- configuração ----------

    private ConfiguracaoLoja loja() {
        return configuracao.findById(ConfiguracaoLoja.ID).orElseGet(() -> { var n = new ConfiguracaoLoja(); n.setId(ConfiguracaoLoja.ID); return n; });
    }

    @Transactional(readOnly = true)
    public Config config() {
        var c = loja();
        return new Config(c.getWhatsappDona(), c.isAlertaProducaoAtivo(), alertas.countByStatus("PENDENTE"), alertas.countByStatus("FALHOU"), provider.configurado());
    }

    /** Liga ou desliga o alerta. Ao ligar, a evidência do consentimento descreve o que de fato aconteceu: quem ativou, quando e para qual número. */
    @Transactional
    public Config salvar(String whatsapp, boolean ativo) {
        var c = loja();
        var tel = whatsapp == null || whatsapp.isBlank() ? null : WhatsappUtil.telefoneE164(whatsapp);
        if (whatsapp != null && !whatsapp.isBlank() && tel == null)
            throw new BusinessException("TELEFONE_INVALIDO", "Informe o WhatsApp da dona com DDD, por exemplo (11) 98765-4321", HttpStatus.BAD_REQUEST);
        if (ativo && tel == null) throw new BusinessException("TELEFONE_OBRIGATORIO", "Informe o WhatsApp da dona para ligar o alerta", HttpStatus.BAD_REQUEST);
        boolean mudouNumero = !Objects.equals(tel, c.getWhatsappDona());
        c.setWhatsappDona(tel);
        if (ativo && (!c.isAlertaProducaoAtivo() || mudouNumero)) {
            var nome = usuarios.findById(audit.userId()).map(u -> u.getNome()).orElse("usuário do sistema");
            var agora = OffsetDateTime.now(clock);
            c.setAlertaAtivadoEm(agora);
            c.setAlertaEvidencia("O número " + tel + " foi cadastrado e o alerta de produção foi ativado em Configurações por " + nome + " em "
                    + agora.atZoneSameInstant(clock.getZone()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ", para receber avisos operacionais da loja.");
        }
        c.setAlertaProducaoAtivo(ativo);
        configuracao.save(c);
        return config();
    }

    // ---------- fila ----------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoCriarOrdem(EventosEstoque.OrdemAutomaticaCriada e) {
        try {
            var c = loja();
            if (!c.isAlertaProducaoAtivo() || c.getWhatsappDona() == null || alertas.existsByIdOrdem(e.ordemId())) return;
            var ordem = ordens.findById(e.ordemId()).orElse(null);
            if (ordem == null) return;
            var a = new AlertaProducao();
            a.setIdOrdem(e.ordemId());
            a.setIdProduto(ordem.getProdutoAcabado().getId());
            a.setProdutoNome(e.produto());
            a.setQuantidade(e.quantidade());
            a.setSaldo(e.saldoComercial());
            a.setEstoqueMinimo(e.estoqueMinimo());
            audit.created(a);
            alertas.save(a);
        } catch (RuntimeException ex) {
            log.error("Não foi possível enfileirar o alerta da ordem {}", e.ordemId(), ex);
        }
    }

    @Transactional
    public int reenviarFalhas() {
        var falhas = alertas.findAllByStatusOrderByCreatedAtAsc("FALHOU");
        for (var a : falhas) { a.setStatus("PENDENTE"); a.setTentativas(0); a.setErroCodigo(null); alertas.save(a); }
        return falhas.size();
    }

    /** Envia, numa única mensagem, tudo o que está pendente. Chamado pela rotina externa (e pelo botão de reenvio). */
    public Processamento processar() {
        var c = loja();
        if (!c.isAlertaProducaoAtivo() || c.getWhatsappDona() == null) return new Processamento(0, 0, "Alerta desligado");
        if (!provider.configurado()) return new Processamento(0, 0, "O gateway de WhatsApp não está configurado no servidor");
        var pendentes = alertas.findAllByStatusOrderByCreatedAtAsc("PENDENTE");
        if (pendentes.isEmpty()) return new Processamento(0, 0, null);
        var agora = ZonedDateTime.now(clock);
        if (!ReativacaoService.dentroDoHorario(agora)) return new Processamento(0, 0, "Fora do horário comercial (08h às 20h): o aviso sai às 08h");
        var inicioMes = agora.toLocalDate().withDayOfMonth(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
        long usadas = mensagens.countByEnviadoEmGreaterThanEqualAndEnviadoEmLessThan(inicioMes, inicioMes.plusMonths(1));
        long teto = (long) (cotaMensal * 0.9);
        if (usadas >= teto) return new Processamento(0, 0, "Cota mensal quase no fim: o aviso espera");

        var linhas = new ArrayList<String>();
        var fotos = new ArrayList<Foto>();
        var principais = imagens.principais().stream().collect(java.util.stream.Collectors.toMap(ProdutoImagemRepository.Resumo::getIdProduto, ProdutoImagemRepository.Resumo::getId, (x, y) -> x));
        int i = 1;
        for (var a : pendentes) {
            Produto p = produtos.findById(a.getIdProduto()).orElse(null);
            linhas.add(linha(i++, a.getProdutoNome(), p == null ? null : p.getCor(), p == null ? null : p.getTamanho(), a.getSaldo(), a.getEstoqueMinimo(), a.getQuantidade()));
            var foto = principais.get(a.getIdProduto());
            if (foto != null && fotos.size() < FOTOS_MAXIMAS) fotos.add(new Foto(foto, a.getProdutoNome()));
        }
        var texto = modelos.renderizar("ALERTA_PRODUCAO_DONA", Map.of("loja", LOJA, "lista_tapetes", montarLista(linhas, TAPETES_NA_LISTA), "link", linkOrdens));
        if (texto.isEmpty()) return new Processamento(0, 0, "A mensagem \"Alerta de produção para a dona\" está desativada");

        var lote = pendentes.get(0).getId();
        var chave = "alerta-producao-" + lote;
        int enviadas = 0;
        try {
            if (!mensagens.existsByIdempotencyKey(chave)) {
                registrar(chave, c.getWhatsappDona(), provider.enviarTexto(c.getWhatsappDona(), texto.get(), FINALIDADE, chave, c.getAlertaEvidencia()), agora);
                enviadas++;
            }
        } catch (GatewayException e) {
            return falhaNoTexto(pendentes, e);
        }
        // As fotos vão depois do texto: se uma falhar, o aviso principal já chegou e os alertas contam como enviados.
        long cabem = Math.max(0, teto - usadas - enviadas);
        int n = 0;
        for (var f : fotos) {
            if (n >= cabem) break;
            var img = imagens.findById(f.imagemId()).orElse(null);
            if (img == null) continue;
            var k = chave + "-foto-" + (++n);
            try {
                if (mensagens.existsByIdempotencyKey(k)) continue;
                registrar(k, c.getWhatsappDona(), provider.enviarImagem(c.getWhatsappDona(), img.getDados(), img.getContentType(), f.nome(), FINALIDADE, k, c.getAlertaEvidencia()), agora);
                enviadas++;
            } catch (GatewayException e) {
                log.warn("Foto do alerta não enviada ({}): {}", e.codigo(), e.getMessage());
                if (e.deveParar()) break;
            }
        }
        for (var a : pendentes) { a.setStatus("ENVIADO"); a.setLote(lote); a.setEnviadoEm(OffsetDateTime.now(clock)); a.setErroCodigo(null); alertas.save(a); }
        return new Processamento(pendentes.size(), enviadas, null);
    }

    private record Foto(String imagemId, String nome) {}

    private Processamento falhaNoTexto(List<AlertaProducao> pendentes, GatewayException e) {
        for (var a : pendentes) {
            a.setTentativas(a.getTentativas() + 1);
            a.setErroCodigo(e.codigo());
            // Erro do próprio conteúdo/consentimento, ou muitas tentativas: para de insistir e deixa o "Reenviar" para a dona.
            if (e.definitivo() || a.getTentativas() >= TENTATIVAS_MAXIMAS) a.setStatus("FALHOU");
            alertas.save(a);
        }
        return new Processamento(0, 0, e.getMessage() + (e.requisicaoId() == null ? "" : " (requisição " + e.requisicaoId() + ")"));
    }

    private void registrar(String chave, String telefone, WhatsAppProvider.Enviada enviada, ZonedDateTime agora) {
        var m = new WhatsappMensagem();
        m.setTelefone(telefone);
        m.setEtapa(0);
        m.setIdempotencyKey(chave);
        m.setMensagemGateway(enviada.mensagemId());
        m.setSituacao(enviada.situacao());
        m.setEnviadoEm(agora.toOffsetDateTime());
        audit.created(m);
        mensagens.save(m);
    }
}

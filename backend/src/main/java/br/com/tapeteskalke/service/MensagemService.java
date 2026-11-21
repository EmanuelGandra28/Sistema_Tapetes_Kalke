package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.MensagemModelo;
import br.com.tapeteskalke.entity.MensagemModeloHistorico;
import br.com.tapeteskalke.entity.Usuario;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.exception.ResourceNotFoundException;
import br.com.tapeteskalke.repository.MensagemModeloHistoricoRepository;
import br.com.tapeteskalke.repository.MensagemModeloRepository;
import br.com.tapeteskalke.repository.UsuarioRepository;
import br.com.tapeteskalke.service.MensagemCatalogo.Definicao;
import br.com.tapeteskalke.service.MensagemCatalogo.Variavel;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Modelos das mensagens automáticas: a dona e a gerente editam o texto; o sistema valida os campos, guarda o histórico
// e é o ÚNICO caminho para montar mensagens ({@link #renderizar}), para nenhum texto ficar fixo no código dos envios.
@Service
public class MensagemService {
    public record VariavelResponse(String nome, String descricao, String exemplo) {}
    public record ModeloResponse(String chave, String nome, String descricao, String tipo, String texto, String textoPadrao, boolean ativo, boolean personalizado,
                                 List<VariavelResponse> variaveis, List<String> obrigatorias, String rodapeSaida, OffsetDateTime atualizadoEm, String atualizadoPor) {}
    public record SalvarResultado(ModeloResponse modelo, List<String> avisos) {}
    public record HistoricoResponse(String id, String textoAnterior, OffsetDateTime alteradoEm, String alteradoPor) {}

    private static final Pattern CAMPO = Pattern.compile("\\{([^{}]*)\\}");
    private static final Pattern LINK = Pattern.compile("(?i)(https?://|www\\.)\\S+");
    private static final Set<String> ENCURTADORES = Set.of("bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "is.gd", "ow.ly", "rb.gy", "shorturl.at", "tiny.cc", "buff.ly");

    private final MensagemModeloRepository repo;
    private final MensagemModeloHistoricoRepository historicos;
    private final UsuarioRepository usuarios;
    private final AuditService audit;

    public MensagemService(MensagemModeloRepository repo, MensagemModeloHistoricoRepository historicos, UsuarioRepository usuarios, AuditService audit) {
        this.repo = repo;
        this.historicos = historicos;
        this.usuarios = usuarios;
        this.audit = audit;
    }

    // ---------- regras puras (testáveis sem banco) ----------

    /** Campos {nome} usados no texto, na ordem em que aparecem. */
    public static List<String> camposUsados(String texto) {
        var campos = new ArrayList<String>();
        var m = CAMPO.matcher(texto);
        while (m.find()) campos.add(m.group(1).trim());
        return campos;
    }

    /** Valida o texto de um gatilho; devolve os AVISOS (não impedem salvar) e lança erro se houver problema. */
    public static List<String> validar(Definicao d, String texto) {
        if (texto == null || texto.isBlank()) throw erro("TEXTO_VAZIO", "O texto da mensagem não pode ficar vazio");
        if (texto.length() > MensagemCatalogo.TAMANHO_MAXIMO)
            throw erro("TEXTO_GRANDE", "A mensagem passou de " + MensagemCatalogo.TAMANHO_MAXIMO + " caracteres (tem " + texto.length() + "). Mensagens longas aumentam o risco de bloqueio do WhatsApp");
        var aceitos = d.variaveis().stream().map(Variavel::nome).collect(Collectors.toSet());
        var desconhecidos = camposUsados(texto).stream().filter(c -> !aceitos.contains(c)).distinct().toList();
        if (!desconhecidos.isEmpty())
            throw erro("CAMPO_DESCONHECIDO", "Campo desconhecido: " + desconhecidos.stream().map(c -> "{" + c + "}").collect(Collectors.joining(", "))
                    + ". Campos aceitos nesta mensagem: " + d.variaveis().stream().map(v -> "{" + v.nome() + "}").collect(Collectors.joining(", ")));
        // Chaves soltas ("{nome" ou "nome}") deixariam a mensagem quebrada para o cliente.
        if (CAMPO.matcher(texto).replaceAll("").matches("(?s).*[{}].*")) throw erro("CHAVES_SOLTAS", "Há uma chave { ou } solta no texto. Os campos devem ficar assim: {nome}");
        var faltando = d.obrigatorias().stream().filter(o -> !camposUsados(texto).contains(o)).toList();
        if (!faltando.isEmpty())
            throw erro("CAMPO_OBRIGATORIO", "Esta mensagem precisa conter: " + faltando.stream().map(c -> "{" + c + "}").collect(Collectors.joining(", ")));
        var avisos = new ArrayList<String>();
        var links = LINK.matcher(texto);
        boolean temLink = false;
        while (links.find()) {
            temLink = true;
            var endereco = links.group().toLowerCase().replaceFirst("^(https?://)?(www\\.)?", "");
            var dominio = endereco.split("[/?#]", 2)[0];
            if (ENCURTADORES.contains(dominio)) throw erro("LINK_ENCURTADO", "Não use links encurtados (" + dominio + "): o WhatsApp costuma bloquear. Use o endereço completo");
        }
        if (temLink && d.tipo() == MensagemCatalogo.Tipo.CAMPANHA) avisos.add("Campanhas com link têm mais chance de serem bloqueadas pelo WhatsApp. Prefira pedir que o cliente responda a mensagem.");
        return avisos;
    }

    /** Troca os campos pelos dados e, em campanhas, anexa a frase de saída. Falta de dado → erro (a mensagem não deve sair quebrada). */
    public static String montar(Definicao d, String texto, Map<String, String> dados) {
        var faltando = camposUsados(texto).stream().filter(c -> dados.get(c) == null || dados.get(c).isBlank()).distinct().toList();
        if (!faltando.isEmpty()) throw erro("DADO_FALTANTE", "Faltam dados para montar a mensagem: " + String.join(", ", faltando));
        var m = CAMPO.matcher(texto);
        var saida = m.replaceAll(r -> java.util.regex.Matcher.quoteReplacement(dados.get(r.group(1).trim())));
        if (d.exigeSaida() && !saida.contains(MensagemCatalogo.RODAPE_SAIDA)) saida = saida.stripTrailing() + "\n\n" + MensagemCatalogo.RODAPE_SAIDA;
        return saida;
    }

    private static BusinessException erro(String codigo, String mensagem) { return new BusinessException(codigo, mensagem, HttpStatus.BAD_REQUEST); }

    private static Definicao definicao(String chave) {
        return MensagemCatalogo.buscar(chave).orElseThrow(() -> new ResourceNotFoundException("Mensagem", chave));
    }

    // ---------- uso pelo resto do sistema ----------

    /** Texto pronto para enviar; vazio se o gatilho estiver desativado. É por aqui que TODO envio de mensagem deve passar. */
    @Transactional
    public Optional<String> renderizar(String chave, Map<String, String> dados) {
        var d = definicao(chave);
        var m = garantir(d);
        return m.isAtivo() ? Optional.of(montar(d, m.getTexto(), dados)) : Optional.empty();
    }

    // ---------- aba Mensagens ----------

    @Transactional
    public List<ModeloResponse> listar() {
        var existentes = repo.findAllByOrderByChaveAsc().stream().collect(Collectors.toMap(MensagemModelo::getChave, x -> x));
        var lista = new ArrayList<MensagemModelo>();
        for (var d : MensagemCatalogo.todas()) lista.add(existentes.containsKey(d.chave()) ? existentes.get(d.chave()) : garantir(d));
        var nomes = nomes(lista);
        return lista.stream().map(m -> resposta(definicao(m.getChave()), m, nomes)).toList();
    }

    @Transactional
    public SalvarResultado salvar(String chave, String texto, Boolean ativo) {
        var d = definicao(chave);
        var m = garantir(d);
        var novo = texto == null ? m.getTexto() : texto.strip();
        var avisos = validar(d, novo);
        if (!novo.equals(m.getTexto())) guardarHistorico(m);
        m.setTexto(novo);
        if (ativo != null) m.setAtivo(ativo);
        audit.changed(m);
        m = repo.save(m);
        return new SalvarResultado(resposta(d, m, nomes(List.of(m))), avisos);
    }

    @Transactional
    public ModeloResponse restaurarPadrao(String chave) {
        var d = definicao(chave);
        var m = garantir(d);
        if (!m.getTexto().equals(d.textoPadrao())) guardarHistorico(m);
        m.setTexto(d.textoPadrao());
        audit.changed(m);
        m = repo.save(m);
        return resposta(d, m, nomes(List.of(m)));
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> historico(String chave) {
        definicao(chave);
        var lista = historicos.findTop20ByChaveOrderByCreatedAtDesc(chave);
        var ids = lista.stream().map(MensagemModeloHistorico::getCreatedBy).filter(Objects::nonNull).collect(Collectors.toSet());
        var nomes = ids.isEmpty() ? Map.<String, String>of() : usuarios.findAllById(ids).stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNome, (a, b) -> a));
        return lista.stream().map(h -> new HistoricoResponse(h.getId(), h.getTextoAnterior(), h.getCreatedAt(), h.getCreatedBy() == null ? null : nomes.get(h.getCreatedBy()))).toList();
    }

    @Transactional
    public ModeloResponse restaurarVersao(String chave, String historicoId) {
        var d = definicao(chave);
        var h = historicos.findById(historicoId).filter(x -> x.getChave().equals(chave)).orElseThrow(() -> new ResourceNotFoundException("Histórico", historicoId));
        validar(d, h.getTextoAnterior()); // a versão antiga ainda precisa ser válida (os campos do gatilho podem ter mudado)
        var m = garantir(d);
        if (!m.getTexto().equals(h.getTextoAnterior())) guardarHistorico(m);
        m.setTexto(h.getTextoAnterior());
        audit.changed(m);
        m = repo.save(m);
        return resposta(d, m, nomes(List.of(m)));
    }

    // ---------- internos ----------

    private MensagemModelo garantir(Definicao d) {
        return repo.findByChave(d.chave()).orElseGet(() -> {
            var m = new MensagemModelo();
            m.setChave(d.chave());
            m.setTexto(d.textoPadrao());
            m.setAtivo(true);
            audit.created(m);
            return repo.save(m);
        });
    }

    private void guardarHistorico(MensagemModelo m) {
        var h = new MensagemModeloHistorico();
        h.setChave(m.getChave());
        h.setTextoAnterior(m.getTexto());
        audit.created(h);
        historicos.save(h);
    }

    private Map<String, String> nomes(List<MensagemModelo> lista) {
        var ids = lista.stream().map(MensagemModelo::getUpdatedBy).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : usuarios.findAllById(ids).stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNome, (a, b) -> a));
    }

    private ModeloResponse resposta(Definicao d, MensagemModelo m, Map<String, String> nomes) {
        return new ModeloResponse(d.chave(), d.nome(), d.descricao(), d.tipo().name(), m.getTexto(), d.textoPadrao(), m.isAtivo(), !m.getTexto().equals(d.textoPadrao()),
                d.variaveis().stream().map(v -> new VariavelResponse(v.nome(), v.descricao(), v.exemplo())).toList(), d.obrigatorias(),
                d.exigeSaida() ? MensagemCatalogo.RODAPE_SAIDA : null, m.getUpdatedAt(), m.getUpdatedBy() == null ? null : nomes.get(m.getUpdatedBy()));
    }
}

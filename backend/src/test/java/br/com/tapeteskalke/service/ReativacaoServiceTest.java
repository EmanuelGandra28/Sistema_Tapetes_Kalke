package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.tapeteskalke.entity.*;
import br.com.tapeteskalke.exception.BusinessException;
import br.com.tapeteskalke.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReativacaoServiceTest {
    private final CampanhaReativacaoRepository campanhas = mock(CampanhaReativacaoRepository.class);
    private final CampanhaDestinatarioRepository destinatarios = mock(CampanhaDestinatarioRepository.class);
    private final WhatsappMensagemRepository mensagens = mock(WhatsappMensagemRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final VendaRepository vendas = mock(VendaRepository.class);
    private final ConsentimentoService consentimentos = mock(ConsentimentoService.class);
    private final MensagemService modelos = mock(MensagemService.class);
    private final WhatsAppProvider provider = mock(WhatsAppProvider.class);
    private final AuditService audit = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Sao_Paulo")); // 12h em São Paulo
    private ReativacaoService service = novo(clock, 500);

    private CampanhaReativacao campanha;
    private CampanhaDestinatario dest;
    private Cliente cliente;

    private ReativacaoService novo(Clock c, int cota) {
        return new ReativacaoService(campanhas, destinatarios, mensagens, clientes, vendas, consentimentos, modelos, provider, audit, c, cota, "segredo");
    }

    @BeforeEach
    void cenario() {
        campanha = new CampanhaReativacao();
        campanha.setId("c1");
        campanha.setNome("Volta");
        campanha.setStatus("APROVADA");
        campanha.setIntervaloDias(5);
        campanha.setLimiteDiario(20);
        cliente = new Cliente();
        cliente.setId("cli1");
        cliente.setNome("Maria Silva");
        cliente.setTelefone("11987654321");
        cliente.setAtivo(true);
        cliente.setStatusCadastral("ATIVO");
        cliente.setUltimaCompra(LocalDate.parse("2026-08-01"));
        cliente.setTotalCompras(new BigDecimal("900"));
        dest = new CampanhaDestinatario();
        dest.setId("d1");
        dest.setIdCampanha("c1");
        dest.setIdCliente("cli1");
        dest.setProximaEtapaEm(OffsetDateTime.parse("2026-11-20T12:00:00Z"));
        dest.setCreatedAt(OffsetDateTime.parse("2026-11-20T12:00:00Z"));
        var consent = new ClienteConsentimento();
        consent.setEvidencia("Autorizou no balcão em 10/11/2026");
        when(provider.configurado()).thenReturn(true);
        when(campanhas.findById("c1")).thenReturn(Optional.of(campanha));
        when(clientes.findById("cli1")).thenReturn(Optional.of(cliente));
        when(consentimentos.vigente("cli1")).thenReturn(Optional.of(consent));
        when(destinatarios.fila(any())).thenReturn(List.of(dest));
        when(destinatarios.findAllByIdCampanha("c1")).thenReturn(List.of(dest));
        when(modelos.renderizar(anyString(), any())).thenReturn(Optional.of("Olá, Maria!"));
        when(provider.enviarTexto(any(), any(), any(), any(), any())).thenReturn(new WhatsAppProvider.Enviada("g-1", "na_fila"));
        when(vendas.historyByClient("cli1")).thenReturn(List.of());
    }

    @Test
    void enviaAPrimeiraMensagemComConsentimentoEChaveDeIdempotencia() {
        var r = service.processarFila();
        assertEquals(1, r.enviadas());
        verify(provider).enviarTexto(eq("+5511987654321"), eq("Olá, Maria!"), eq("marketing_reativacao"), eq("reativacao-d1-1"), eq("Autorizou no balcão em 10/11/2026"));
        assertEquals(1, dest.getEtapaAtual());
        assertEquals("ATIVO", dest.getStatus());
        assertEquals(OffsetDateTime.now(clock).plusDays(5).toInstant(), dest.getProximaEtapaEm().toInstant());
        var m = ArgumentCaptor.forClass(WhatsappMensagem.class);
        verify(mensagens).save(m.capture());
        assertEquals("g-1", m.getValue().getMensagemGateway());
        // os dados da mensagem chegam ao modelo (nome só o primeiro, dias sem comprar)
        var dados = ArgumentCaptor.forClass(Map.class);
        verify(modelos).renderizar(eq("REATIVACAO_1"), dados.capture());
        assertEquals("Maria", dados.getValue().get("nome"));
        assertEquals("112", dados.getValue().get("dias_sem_comprar"));
    }

    @Test
    void aTerceiraMensagemConcluiOClienteEACampanha() {
        dest.setEtapaAtual(2);
        service.processarFila();
        assertEquals(3, dest.getEtapaAtual());
        assertEquals("CONCLUIDO", dest.getStatus());
        assertEquals("CONCLUIDA", campanha.getStatus());
    }

    @Test
    void foraDoHorarioComercialNaoEnviaNada() {
        var noite = Clock.fixed(Instant.parse("2026-11-21T01:00:00Z"), ZoneId.of("America/Sao_Paulo")); // 22h em São Paulo
        var r = novo(noite, 500).processarFila();
        assertEquals(0, r.enviadas());
        assertNotNull(r.motivoParada());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
        assertFalse(ReativacaoService.dentroDoHorario(ZonedDateTime.of(2026, 11, 21, 7, 59, 0, 0, ZoneId.of("America/Sao_Paulo"))));
        assertTrue(ReativacaoService.dentroDoHorario(ZonedDateTime.of(2026, 11, 21, 8, 0, 0, 0, ZoneId.of("America/Sao_Paulo"))));
        assertFalse(ReativacaoService.dentroDoHorario(ZonedDateTime.of(2026, 11, 21, 20, 0, 0, 0, ZoneId.of("America/Sao_Paulo"))));
    }

    @Test
    void semAChaveDoGatewayNaoEnviaNada() {
        when(provider.configurado()).thenReturn(false);
        var r = service.processarFila();
        assertEquals(0, r.enviadas());
        assertNotNull(r.motivoParada());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
    }

    @Test
    void respeitaOLimiteDiarioDaCampanha() {
        campanha.setLimiteDiario(5);
        when(mensagens.enviadasDaCampanhaDesde(eq("c1"), any())).thenReturn(5L);
        var r = service.processarFila();
        assertEquals(0, r.enviadas());
        assertEquals(1, r.puladas());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
    }

    @Test
    void paraQuandoACotaMensalChegaAoBloqueio() {
        when(mensagens.countByEnviadoEmGreaterThanEqualAndEnviadoEmLessThan(any(), any())).thenReturn(450L); // 90% de 500
        var r = service.processarFila();
        assertEquals(0, r.enviadas());
        assertTrue(r.motivoParada().contains("Cota"));
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
    }

    @Test
    void semConsentimentoNuncaEnvia() {
        when(consentimentos.vigente("cli1")).thenReturn(Optional.empty());
        service.processarFila();
        assertEquals("CANCELADO", dest.getStatus());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
    }

    @Test
    void clienteQueJaVoltouAComprarSaiDaCampanha() {
        cliente.setUltimaCompra(LocalDate.parse("2026-11-20"));
        service.processarFila();
        assertEquals("RECOMPROU", dest.getStatus());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
    }

    @Test
    void mensagemDesativadaNaAbaMensagensFicaEmEspera() {
        when(modelos.renderizar(anyString(), any())).thenReturn(Optional.empty());
        var r = service.processarFila();
        assertEquals(1, r.puladas());
        assertEquals(0, dest.getEtapaAtual());
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
    }

    @Test
    void semSaldoOuSemPermissaoParaTudoENaoMarcaFalha() {
        when(provider.enviarTexto(any(), any(), any(), any(), any())).thenThrow(new GatewayException(402, "LIMITE_DO_PLANO_ATINGIDO", "Limite atingido", "r-1"));
        var r = service.processarFila();
        assertEquals(0, r.enviadas());
        assertTrue(r.motivoParada().contains("Limite atingido"));
        assertEquals("ATIVO", dest.getStatus());
    }

    @Test
    void erroDefinitivoDoGatewayMarcaFalhaSoDaquelePessoa() {
        when(provider.enviarTexto(any(), any(), any(), any(), any())).thenThrow(new GatewayException(422, "CONSENTIMENTO_AUSENTE", "Sem consentimento", null));
        var r = service.processarFila();
        assertEquals(1, r.falhas());
        assertEquals("FALHOU", dest.getStatus());
        assertNull(r.motivoParada());
    }

    @Test
    void mensagemJaEnviadaNaoEnviaDeNovo() {
        when(mensagens.existsByIdempotencyKey("reativacao-d1-1")).thenReturn(true);
        service.processarFila();
        verify(provider, never()).enviarTexto(any(), any(), any(), any(), any());
        assertEquals(1, dest.getEtapaAtual()); // só alcança o estado
    }

    @Test
    void faixasDeValorECicloDeRecompra() {
        // 10 clientes: 2 OURO (20%), 3 PRATA (30%), 5 BRONZE
        var faixas = new ArrayList<String>();
        for (int i = 0; i < 10; i++) faixas.add(ReativacaoService.faixaValor(i, 10));
        assertEquals(List.of("OURO", "OURO", "PRATA", "PRATA", "PRATA", "BRONZE", "BRONZE", "BRONZE", "BRONZE", "BRONZE"), faixas);
        assertEquals("OURO", ReativacaoService.faixaValor(0, 1));
        assertEquals("EM_RISCO", ReativacaoService.ciclo(70));
        assertEquals("INATIVO", ReativacaoService.ciclo(100));
        assertEquals("INATIVO_LONGO", ReativacaoService.ciclo(200));
    }

    @Test
    void respostaPedindoParaSairRevogaAAutorizacaoEInterrompe() {
        dest.setUltimoEnvioEm(OffsetDateTime.now(clock).minusDays(1));
        dest.setEtapaAtual(1);
        when(destinatarios.findAllByUltimoEnvioEmAfter(any())).thenReturn(List.of(dest));
        assertTrue(service.registrarResposta("+5511987654321", "SAIR", null));
        assertEquals("SAIU", dest.getStatus());
        assertEquals("PEDIU_SAIR", dest.getRespostaClasse());
        verify(consentimentos).revogarPorTelefone(eq("+5511987654321"), anyString());
    }

    @Test
    void respostaInteressadaParaAsProximasMensagens() {
        dest.setUltimoEnvioEm(OffsetDateTime.now(clock).minusDays(1));
        when(destinatarios.findAllByUltimoEnvioEmAfter(any())).thenReturn(List.of(dest));
        assertTrue(service.registrarResposta("+5511987654321", "Quero ver as opções", null));
        assertEquals("RESPONDEU", dest.getStatus());
        assertEquals("INTERESSADO", dest.getRespostaClasse());
        assertFalse(service.registrarResposta("+5521999999999", "oi", null)); // telefone desconhecido
    }

    @Test
    void campanhaSoComecaDepoisDeAprovadaEUmaVez() {
        campanha.setStatus("RASCUNHO");
        when(campanhas.save(any(CampanhaReativacao.class))).thenAnswer(i -> i.getArgument(0));
        when(audit.userId()).thenReturn("u1");
        assertEquals("APROVADA", service.aprovar("c1").status());
        assertEquals("u1", campanha.getAprovadaPor());
        assertThrows(BusinessException.class, () -> service.aprovar("c1"));
    }

    @Test
    void naoCriaCampanhaComClienteForaDosCandidatos() {
        when(consentimentos.idsComConsentimento()).thenReturn(Set.of()); // ninguém autorizou
        when(clientes.findAll()).thenReturn(List.of(cliente));
        assertThrows(BusinessException.class, () -> service.criar("Teste", List.of("cli1"), 5, 20));
        verify(campanhas, never()).save(any());
    }

    @Test
    void candidatosSaoSoQuemAutorizouEstaSemComprarETemTelefone() {
        when(consentimentos.idsComConsentimento()).thenReturn(Set.of("cli1"));
        when(clientes.findAll()).thenReturn(List.of(cliente));
        var lista = service.candidatos();
        assertEquals(1, lista.size());
        assertEquals("OURO", lista.get(0).valor());
        cliente.setUltimaCompra(LocalDate.parse("2026-11-01")); // comprou este mês
        assertTrue(service.candidatos().isEmpty());
        cliente.setUltimaCompra(LocalDate.parse("2026-08-01"));
        cliente.setTelefone("123");
        assertTrue(service.candidatos().isEmpty());
        cliente.setTelefone("11987654321");
        when(destinatarios.clientesEmAndamento()).thenReturn(List.of("cli1")); // já está em outra campanha
        assertTrue(service.candidatos().isEmpty());
    }
}

package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.tapeteskalke.service.WhatsappUtil.Resposta;
import org.junit.jupiter.api.Test;

class WhatsappUtilTest {
    @Test
    void telefoneViraFormatoInternacional() {
        assertEquals("+5511987654321", WhatsappUtil.telefoneE164("(11) 98765-4321"));
        assertEquals("+551134567890", WhatsappUtil.telefoneE164("11 3456-7890"));
        assertEquals("+5511987654321", WhatsappUtil.telefoneE164("+55 11 98765-4321"));
        assertEquals("+5511987654321", WhatsappUtil.telefoneE164("011987654321"));
        assertNull(WhatsappUtil.telefoneE164(""));
        assertNull(WhatsappUtil.telefoneE164(null));
        assertNull(WhatsappUtil.telefoneE164("1234"));
    }

    @Test
    void classificaAsRespostasDosClientes() {
        assertEquals(Resposta.PEDIU_SAIR, WhatsappUtil.classificar("SAIR"));
        assertEquals(Resposta.PEDIU_SAIR, WhatsappUtil.classificar("Por favor, parem de me enviar. Pare"));
        assertEquals(Resposta.PEDIU_SAIR, WhatsappUtil.classificar("quero cancelar"));
        assertEquals(Resposta.SEM_INTERESSE, WhatsappUtil.classificar("Não tenho interesse, obrigada"));
        assertEquals(Resposta.SEM_INTERESSE, WhatsappUtil.classificar("agora não"));
        assertEquals(Resposta.SEM_INTERESSE, WhatsappUtil.classificar("Não"));
        assertEquals(Resposta.INTERESSADO, WhatsappUtil.classificar("Sim, quero ver as opções"));
        assertEquals(Resposta.INTERESSADO, WhatsappUtil.classificar("Qual o preço do tapete?"));
        assertEquals(Resposta.INTERESSADO, WhatsappUtil.classificar("Orçamento por favor"));
        assertEquals(Resposta.OUTRO, WhatsappUtil.classificar("bom dia"));
        assertEquals(Resposta.OUTRO, WhatsappUtil.classificar(null));
    }

    @Test
    void pedirParaSairSempreVenceDeOutraIntencao() {
        assertEquals(Resposta.PEDIU_SAIR, WhatsappUtil.classificar("sim, mas quero sair da lista"));
    }

    @Test
    void assinaturaDoWebhookConfereComCorpoBrutoETempoConstante() {
        var corpo = "{\"evento_id\":\"e1\",\"evento\":\"mensagem.situacao\"}";
        var assinatura = WhatsappUtil.assinar("segredo", "1700000000", corpo);
        assertTrue(assinatura.startsWith("sha256="));
        assertTrue(WhatsappUtil.assinaturaValida("segredo", "1700000000", corpo, assinatura));
        assertFalse(WhatsappUtil.assinaturaValida("outro", "1700000000", corpo, assinatura));
        assertFalse(WhatsappUtil.assinaturaValida("segredo", "1700000001", corpo, assinatura));
        assertFalse(WhatsappUtil.assinaturaValida("segredo", "1700000000", corpo + " ", assinatura));
        assertFalse(WhatsappUtil.assinaturaValida("", "1700000000", corpo, assinatura));
        assertFalse(WhatsappUtil.assinaturaValida("segredo", "1700000000", corpo, null));
    }
}

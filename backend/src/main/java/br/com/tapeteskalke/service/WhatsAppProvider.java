package br.com.tapeteskalke.service;

// Porta de saída das mensagens de WhatsApp. Hoje quem implementa é o Orquestra WhatsApp Gateway; trocar de provedor é trocar só esta classe.
public interface WhatsAppProvider {
    record Enviada(String mensagemId, String situacao) {}

    /** Chave de API presente no servidor. Sem ela nada é enviado (o sistema funciona normalmente, só sem mensagens). */
    boolean configurado();

    /**
     * Envia um texto. {@code destinatario} em formato internacional (+55...). {@code evidencia} é a prova do consentimento
     * escrita por uma pessoa; o provedor a recebe junto no primeiro envio. A chave de idempotência garante que repetir não duplica.
     */
    Enviada enviarTexto(String destinatario, String texto, String finalidade, String idempotencyKey, String evidencia);
}

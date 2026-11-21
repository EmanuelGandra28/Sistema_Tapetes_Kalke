package br.com.tapeteskalke.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// Regras puras (sem banco nem rede) do WhatsApp: telefone, classificação de respostas e assinatura dos webhooks.
public final class WhatsappUtil {
    private WhatsappUtil() {}

    public enum Resposta { INTERESSADO, SEM_INTERESSE, PEDIU_SAIR, OUTRO }

    /** Telefone brasileiro em formato internacional (+5511999999999); null se não parecer um telefone válido. */
    public static String telefoneE164(String telefone) {
        if (telefone == null) return null;
        var d = telefone.replaceAll("\\D", "").replaceFirst("^0+", "");
        if (d.length() == 10 || d.length() == 11) d = "55" + d;
        return d.startsWith("55") && (d.length() == 12 || d.length() == 13) ? "+" + d : null;
    }

    private static String simples(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    /** Lê a resposta do cliente: pediu para sair, não tem interesse, tem interesse ou outra coisa. "Sair" sempre vence. */
    public static Resposta classificar(String texto) {
        var t = simples(texto);
        if (t.matches("(?s).*\\b(sair|parar|pare|descadastrar|descadastre|remover|remova|cancelar|nao quero mais receber|nao me envie|nao envie)\\b.*")) return Resposta.PEDIU_SAIR;
        if (t.matches("(?s).*\\b(nao quero|nao tenho interesse|sem interesse|nao preciso|agora nao|nao obrigad[oa]|nao,? obrigad[oa])\\b.*") || t.equals("nao")) return Resposta.SEM_INTERESSE;
        if (t.matches("(?s).*\\b(sim|quero|tenho interesse|interessad[oa]|gostaria|pode ser|preco|precos|valor|valores|orcamento|catalogo|novidades|me mostra|me envia|me manda|opcoes|opcao)\\b.*")) return Resposta.INTERESSADO;
        return Resposta.OUTRO;
    }

    /** Assinatura do webhook: sha256= + HMAC-SHA256 hexadecimal de "timestamp.corpo". */
    public static String assinar(String segredo, String timestamp, String corpoBruto) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal((timestamp + "." + corpoBruto).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC indisponível", e);
        }
    }

    /** Confere a assinatura com comparação em tempo constante (não vaza onde o texto difere). */
    public static boolean assinaturaValida(String segredo, String timestamp, String corpoBruto, String recebida) {
        if (segredo == null || segredo.isBlank() || timestamp == null || recebida == null) return false;
        return MessageDigest.isEqual(assinar(segredo, timestamp, corpoBruto).getBytes(StandardCharsets.UTF_8), recebida.getBytes(StandardCharsets.UTF_8));
    }
}

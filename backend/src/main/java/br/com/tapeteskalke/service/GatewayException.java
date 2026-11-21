package br.com.tapeteskalke.service;

// Erro devolvido pelo gateway (application/problem+json): status HTTP, código estável, título em português e id da requisição (para suporte).
public class GatewayException extends RuntimeException {
    private final int status;
    private final String codigo;
    private final String requisicaoId;

    public GatewayException(int status, String codigo, String titulo, String requisicaoId) {
        super(titulo);
        this.status = status;
        this.codigo = codigo;
        this.requisicaoId = requisicaoId;
    }

    public int status() { return status; }

    public String codigo() { return codigo; }

    public String requisicaoId() { return requisicaoId; }

    /** 402 (sem saldo) e 403 (sem permissão / conta suspensa): parar tudo e avisar, nunca insistir. */
    public boolean deveParar() { return status == 402 || status == 403; }

    /** Erro da mensagem em si (dado ou consentimento): não adianta repetir. */
    public boolean definitivo() { return status == 400 || status == 409 || status == 422; }
}

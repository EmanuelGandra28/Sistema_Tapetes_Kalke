package br.com.tapeteskalke.service;

import br.com.tapeteskalke.exception.BusinessException;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// Botão "Atualizar site": dispara o workflow de publicação do site de portfólio no GitHub.
// O token fica só numa variável de ambiente do servidor (nunca no código nem no banco).
@Service
public class PublicacaoSiteService {
    static final Duration ESPERA_ENTRE_PEDIDOS = Duration.ofMinutes(2);

    /** Dispara o workflow e devolve o status HTTP da resposta do GitHub (204 = aceito). */
    public interface GithubClient {
        int dispararWorkflow(String repo, String workflow, String token) throws IOException;
    }

    private final GithubClient github;
    private final Clock clock;
    private final String token;
    private final String repo;
    private final String workflow;
    private Instant ultimoPedido;

    public PublicacaoSiteService(GithubClient github, Clock clock, @Value("${app.portfolio.github-token:}") String token,
                                 @Value("${app.portfolio.repo}") String repo, @Value("${app.portfolio.workflow}") String workflow) {
        this.github = github;
        this.clock = clock;
        this.token = token;
        this.repo = repo;
        this.workflow = workflow;
    }

    public synchronized void publicar() {
        if (token == null || token.isBlank())
            throw new BusinessException("SITE_NAO_CONFIGURADO", "A atualização do site ainda não foi configurada", HttpStatus.BAD_REQUEST);
        var agora = Instant.now(clock);
        if (ultimoPedido != null && agora.isBefore(ultimoPedido.plus(ESPERA_ENTRE_PEDIDOS)))
            throw new BusinessException("SITE_AGUARDE", "O site acabou de ser atualizado. Aguarde alguns minutos para pedir de novo", HttpStatus.TOO_MANY_REQUESTS);
        int status;
        try {
            status = github.dispararWorkflow(repo, workflow, token);
        } catch (IOException e) {
            throw falhou();
        }
        if (status != 204) throw falhou();
        ultimoPedido = agora;
    }

    private BusinessException falhou() {
        return new BusinessException("SITE_FALHOU", "Não foi possível atualizar o site agora. Tente de novo em alguns minutos", HttpStatus.BAD_GATEWAY);
    }
}

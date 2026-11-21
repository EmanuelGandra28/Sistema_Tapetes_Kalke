package br.com.tapeteskalke.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

// Chamada real à API do GitHub (workflow_dispatch). Mantida pequena de propósito: a regra fica em PublicacaoSiteService.
@Component
public class GithubHttpClient implements PublicacaoSiteService.GithubClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public int dispararWorkflow(String repo, String workflow, String token) throws IOException {
        var request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + repo + "/actions/workflows/" + workflow + "/dispatches"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "tapetes-kalke-api")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"ref\":\"main\"}"))
                .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Chamada ao GitHub interrompida", e);
        }
    }
}

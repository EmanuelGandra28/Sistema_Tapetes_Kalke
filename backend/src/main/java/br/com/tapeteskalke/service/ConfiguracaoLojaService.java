package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.ConfiguracaoLoja;
import br.com.tapeteskalke.repository.ConfiguracaoLojaRepository;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Dados da loja usados no QR Code PIX. pixConfigurado = chave, nome e cidade preenchidos.
@Service
public class ConfiguracaoLojaService {
    public record Dados(@Size(max = 77) String pixChave, @Size(max = 25) String pixNomeRecebedor, @Size(max = 15) String pixCidade, boolean pixConfigurado) {}

    private final ConfiguracaoLojaRepository repo;

    public ConfiguracaoLojaService(ConfiguracaoLojaRepository repo) {
        this.repo = repo;
    }

    private static String limpo(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    private static Dados dados(ConfiguracaoLoja c) {
        return new Dados(c.getPixChave(), c.getPixNomeRecebedor(), c.getPixCidade(),
                c.getPixChave() != null && c.getPixNomeRecebedor() != null && c.getPixCidade() != null);
    }

    @Transactional(readOnly = true)
    public Dados dados() {
        return repo.findById(ConfiguracaoLoja.ID).map(ConfiguracaoLojaService::dados).orElse(new Dados(null, null, null, false));
    }

    @Transactional
    public Dados salvar(Dados d) {
        var c = repo.findById(ConfiguracaoLoja.ID).orElseGet(() -> { var n = new ConfiguracaoLoja(); n.setId(ConfiguracaoLoja.ID); return n; });
        c.setPixChave(limpo(d.pixChave()));
        c.setPixNomeRecebedor(limpo(d.pixNomeRecebedor()));
        c.setPixCidade(limpo(d.pixCidade()));
        return dados(repo.save(c));
    }
}

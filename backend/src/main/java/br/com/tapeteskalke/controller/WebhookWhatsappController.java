package br.com.tapeteskalke.controller;

import br.com.tapeteskalke.service.WebhookWhatsappService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Endereço que se cadastra no painel do gateway (Webhooks): https://<api>/api/v1/webhooks/orquestra
// Sem login (o gateway não tem usuário): a segurança é a assinatura HMAC conferida sobre o corpo bruto.
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookWhatsappController {
    private final WebhookWhatsappService service;

    public WebhookWhatsappController(WebhookWhatsappService service) {
        this.service = service;
    }

    @PostMapping("/orquestra")
    public ResponseEntity<Void> receber(@RequestBody String corpo, @RequestHeader(value = "X-Orquestra-Timestamp", required = false) String timestamp,
                                        @RequestHeader(value = "X-Orquestra-Assinatura", required = false) String assinatura) {
        return switch (service.processar(corpo, timestamp, assinatura)) {
            case PROCESSADO, DUPLICADO -> ResponseEntity.noContent().build();
            case CORPO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            case ASSINATURA_INVALIDA, FORA_DA_JANELA -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        };
    }
}

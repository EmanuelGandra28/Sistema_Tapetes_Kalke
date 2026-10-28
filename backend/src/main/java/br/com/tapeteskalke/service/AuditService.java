package br.com.tapeteskalke.service;

import br.com.tapeteskalke.entity.BaseAuditEntity;
import br.com.tapeteskalke.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final CurrentUser current;

    public AuditService(CurrentUser current) {
        this.current = current;
    }

    public String userId() {
        return current.require().id();
    }

    public void created(BaseAuditEntity e) {
        var id = userId();
        e.setCreatedBy(id);
        e.setUpdatedBy(id);
    }

    public void changed(BaseAuditEntity e) {
        e.setUpdatedBy(userId());
    }
}
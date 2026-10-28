package br.com.tapeteskalke.entity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@MappedSuperclass @Getter @Setter public abstract class BaseAuditEntity {
 @Id @Column(length=38) private String id;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @Column(name="created_by",length=38) private String createdBy;
 @Column(name="updated_by",length=38) private String updatedBy;
 @PrePersist void createAudit(){ if(id==null) id=UUID.randomUUID().toString(); var now=OffsetDateTime.now(ZoneOffset.UTC); if(createdAt==null)createdAt=now; updatedAt=now; }
 @PreUpdate void updateAudit(){updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
}
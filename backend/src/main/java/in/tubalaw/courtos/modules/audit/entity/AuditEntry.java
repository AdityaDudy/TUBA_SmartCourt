package in.tubalaw.courtos.modules.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(nullable = false)
    private String action;

    private String entity;

    @Column(name = "entity_id")
    private String entityId;

    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(length = 20)
    @Builder.Default
    private String risk = "LOW";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

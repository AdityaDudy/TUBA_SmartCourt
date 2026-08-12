package in.tubalaw.courtos.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "ip_whitelist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpWhitelistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "ip_address", nullable = false, length = 100)
    private String ipAddress;

    @Column(length = 200)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

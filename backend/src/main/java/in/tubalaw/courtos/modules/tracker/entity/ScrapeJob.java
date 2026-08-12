package in.tubalaw.courtos.modules.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "scrape_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScrapeJob {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    @Builder.Default
    private String tenantId = "default";

    @Column(nullable = false, length = 20)
    private String cnr;

    /** PENDING, RUNNING, DONE, FAILED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "initiated_by_user_id")
    private Long initiatedByUserId;

    @Column(name = "force_refresh", nullable = false)
    @Builder.Default
    private boolean forceRefresh = false;

    /** Raw normalized JSON result from the provider, stored for full snapshot history */
    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}

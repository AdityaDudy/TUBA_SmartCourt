package in.tubalaw.courtos.modules.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "case_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseOrder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "tracked_case_id", nullable = false)
    private Long trackedCaseId;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "order_no", length = 100)
    private String orderNo;

    @Column(name = "order_type", length = 100)
    private String orderType;

    @Column(name = "order_category", length = 20)
    private String orderCategory;

    @Column(name = "s3_key", length = 1000)
    private String s3Key;

    @Column(name = "s3_url", length = 2000)
    private String s3Url;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    /** SHA-256 hex digest — used to skip re-downloading unchanged PDFs */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /** Original eCourts URL stored for traceability, never exposed as a live download link */
    @Column(name = "external_url", length = 2000)
    private String externalUrl;

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

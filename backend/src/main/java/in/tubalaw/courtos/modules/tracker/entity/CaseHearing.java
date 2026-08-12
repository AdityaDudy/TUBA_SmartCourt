package in.tubalaw.courtos.modules.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "case_hearings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseHearing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "tracked_case_id", nullable = false)
    private Long trackedCaseId;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "judge", length = 500)
    private String judge;

    @Column(name = "purpose_of_hearing", length = 500)
    private String purposeOfHearing;

    @Column(name = "next_hearing_date")
    private LocalDate nextHearingDate;

    @Column(name = "business_remarks", columnDefinition = "text")
    private String businessRemarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

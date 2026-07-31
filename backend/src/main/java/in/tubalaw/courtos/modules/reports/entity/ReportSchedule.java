package in.tubalaw.courtos.modules.reports.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportSchedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "filters_used")
    private String filtersUsed;

    @Column(nullable = false)
    private String frequency;

    @Column(name = "email_recipient", nullable = false)
    private String emailRecipient;

    @Column(name = "last_run")
    private LocalDateTime lastRun;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

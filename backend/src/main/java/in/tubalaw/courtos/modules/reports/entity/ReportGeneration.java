package in.tubalaw.courtos.modules.reports.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_generations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportGeneration {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "filters_used")
    private String filtersUsed;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private String format;
}

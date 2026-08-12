package in.tubalaw.courtos.modules.causelist.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "hearings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hearing extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matter_id")
    private Long matterId;

    @Column(name = "case_title", nullable = false)
    private String caseTitle;

    @Column(name = "case_no")
    private String caseNo;

    private String court;
    private String bench;

    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @Column(name = "hearing_time")
    private String hearingTime;

    private String stage;
    private String advocate;

    @Builder.Default private String status = "Scheduled";

    private String result;

    @Column(name = "next_date")
    private LocalDate nextDate;

    private String notes;
}

package in.tubalaw.courtos.modules.tracker.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tracked_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrackedCase extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String cnr;

    @Column(name = "case_type", length = 200)
    private String caseType;

    @Column(name = "filing_no", length = 100)
    private String filingNo;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "registration_no", length = 100)
    private String registrationNo;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "court_name", length = 500)
    private String courtName;

    @Column(name = "court_complex", length = 500)
    private String courtComplex;

    @Column(name = "judge_name", length = 500)
    private String judgeName;

    /** PENDING, DISPOSED, STAYED, DISMISSED, TRANSFERRED */
    @Column(name = "case_status", length = 50)
    private String caseStatus;

    @Column(name = "stage_of_case", length = 200)
    private String stageOfCase;

    @Column(name = "next_hearing_date")
    private LocalDate nextHearingDate;

    @Column(name = "petitioners", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] petitioners;

    @Column(name = "respondents", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] respondents;

    @Column(name = "petitioner_advocates", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] petitionerAdvocates;

    @Column(name = "respondent_advocates", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] respondentAdvocates;

    @Column(name = "acts_and_sections", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] actsAndSections;

    @Column(name = "fir_no", length = 100)
    private String firNo;

    @Column(name = "fir_year", length = 10)
    private String firYear;

    @Column(name = "police_station", length = 300)
    private String policeStation;

    @Column(name = "matter_id")
    private Long matterId;

    @Column(name = "last_fetched_at")
    private Instant lastFetchedAt;

    @Column(name = "snapshot_version", nullable = false)
    @Builder.Default
    private Integer snapshotVersion = 0;
}

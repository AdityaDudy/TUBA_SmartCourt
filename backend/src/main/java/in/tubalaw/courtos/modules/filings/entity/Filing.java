package in.tubalaw.courtos.modules.filings.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "filings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Filing extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String title;
    @Column(name = "matter_id") private Long matterId;
    @Column(name = "matter_title") private String matterTitle;
    private String court;
    @Column(name = "filing_type") private String filingType;
    private String stage;
    @Builder.Default private String status = "Draft";
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(name = "filed_date") private LocalDate filedDate;
    private String advocate;
    private String notes;
    @Column(name = "s3_url") private String s3Url;
    /** "task" = created from task submission, "manual" = created via Filings form */
    private String source;
}

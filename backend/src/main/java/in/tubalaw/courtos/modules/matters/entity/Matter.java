package in.tubalaw.courtos.modules.matters.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "matters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Matter extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "case_no")
    private String caseNo;

    @Column(name = "cnr_number")
    private String cnrNumber;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;

    private String court;
    private String type;
    private String area;

    @Column(name = "next_hearing")
    private LocalDate nextHearing;

    private String advocate;

    @Builder.Default private String status = "Active";
    private String stage;
    private String outcome;

    @Column(name = "background")
    private String bg;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "opposite_party")
    private String oppositeParty;

    private String notes;
    private String priority;

    @Column(name = "co_counsel")
    private String coCounsel;

    @Column(name = "opposing_counsel")
    private String opposingCounsel;

    @Column(name = "limitation_deadline")
    private LocalDate limitationDeadline;

    @Column(name = "related_matter_id")
    private Long relatedMatterId;
}

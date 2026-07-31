package in.tubalaw.courtos.modules.billing.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matter_id")
    private Long matterId;

    @Column(name = "matter_title")
    private String matterTitle;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(nullable = false)
    private String category;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate date;

    @Builder.Default
    private boolean billable = false;

    @Builder.Default
    private boolean invoiced = false;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "receipt_path", columnDefinition = "TEXT")
    private String receiptPath;
}

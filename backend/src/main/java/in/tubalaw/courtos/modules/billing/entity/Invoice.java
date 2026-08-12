package in.tubalaw.courtos.modules.billing.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no")
    private String invoiceNo;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "matter_id")
    private Long matterId;

    @Column(name = "matter_title")
    private String matterTitle;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "paid_amount")
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** Ensure BigDecimal fields are never null after JPA hydration */
    @PostLoad
    public void initDefaults() {
        if (amount    == null) amount    = BigDecimal.ZERO;
        if (paidAmount== null) paidAmount= BigDecimal.ZERO;
    }

    @Builder.Default private String status = "Draft";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
    @Builder.Default
    private java.util.List<InvoiceLineItem> lineItems = new java.util.ArrayList<>();
}

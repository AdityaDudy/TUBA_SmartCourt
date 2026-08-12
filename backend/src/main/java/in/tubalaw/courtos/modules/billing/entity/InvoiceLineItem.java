package in.tubalaw.courtos.modules.billing.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceLineItem extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "fee_type")
    private String feeType;

    @Column(nullable = false)
    private String description;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "source_reference")
    private String sourceReference;
}

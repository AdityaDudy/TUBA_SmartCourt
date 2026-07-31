package in.tubalaw.courtos.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PendingBillableDto {
    private String id; // e.g. "filing_12"
    private String type; // "Filing", "Hearing", "Task", "Expense"
    private String title;
    private String description;
    private BigDecimal suggestedAmount;
    private LocalDate date;
    private Long matterId;
    private String matterTitle;
    private Long clientId;
    private String clientName;
}

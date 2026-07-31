package in.tubalaw.courtos.modules.reports.builder;

import in.tubalaw.courtos.modules.billing.entity.Invoice;
import in.tubalaw.courtos.modules.reports.dto.ReportTable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RevenueReportBuilder {

    public ReportTable build(List<Invoice> filteredInvoices, String filterSummary) {
        List<String> headers = List.of("Invoice No", "Client Name", "Total Amount (INR)", "Paid Amount (INR)", "Outstanding Balance", "Status");
        List<List<Object>> rows = new ArrayList<>();

        for (Invoice inv : filteredInvoices) {
            String invNo = inv.getInvoiceNo() != null ? inv.getInvoiceNo() : "INV-" + inv.getId();
            String client = inv.getClientName() != null ? inv.getClientName() : "General Client";
            double amount = inv.getAmount() != null ? inv.getAmount().doubleValue() : 0.0;
            double paid = inv.getPaidAmount() != null ? inv.getPaidAmount().doubleValue() : 0.0;
            double balance = Math.max(0.0, amount - paid);
            String status = inv.getStatus() != null ? inv.getStatus() : "Unpaid";

            rows.add(List.of(invNo, client, String.format("INR %,.0f", amount), String.format("INR %,.0f", paid), String.format("INR %,.0f", balance), status));
        }

        return ReportTable.builder()
                .title("Revenue & Invoice Analysis Report")
                .filterSummary(filterSummary)
                .headers(headers)
                .rows(rows)
                .build();
    }
}

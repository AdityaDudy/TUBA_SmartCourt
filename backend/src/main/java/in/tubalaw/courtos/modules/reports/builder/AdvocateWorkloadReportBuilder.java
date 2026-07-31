package in.tubalaw.courtos.modules.reports.builder;

import in.tubalaw.courtos.modules.billing.entity.Invoice;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.reports.dto.ReportTable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AdvocateWorkloadReportBuilder {

    public ReportTable build(List<Matter> filteredMatters, List<Invoice> filteredInvoices, String filterSummary) {
        List<String> headers = List.of("Advocate Name", "Active Cases", "Disposed Cases", "Win Rate (%)", "Revenue Billed (INR)");
        List<List<Object>> rows = new ArrayList<>();

        Map<String, List<Matter>> groupedByAdvocate = filteredMatters.stream()
                .filter(m -> m.getAdvocate() != null && !m.getAdvocate().isBlank())
                .collect(Collectors.groupingBy(Matter::getAdvocate));

        for (Map.Entry<String, List<Matter>> entry : groupedByAdvocate.entrySet()) {
            String adv = entry.getKey();
            List<Matter> advMatters = entry.getValue();

            long activeCount = advMatters.stream().filter(m -> !"Disposed".equalsIgnoreCase(m.getStatus()) && !"Closed".equalsIgnoreCase(m.getStatus())).count();
            List<Matter> disposed = advMatters.stream().filter(m -> "Disposed".equalsIgnoreCase(m.getStatus())).toList();
            long won = disposed.stream().filter(m -> "Won".equalsIgnoreCase(m.getOutcome())).count();
            long lost = disposed.stream().filter(m -> "Lost".equalsIgnoreCase(m.getOutcome())).count();
            int winRate = (won + lost > 0) ? (int) Math.round((double) won / (won + lost) * 100) : 0;

            double totalRev = filteredInvoices.stream()
                    .filter(inv -> inv.getMatterId() != null && advMatters.stream().anyMatch(m -> m.getId().equals(inv.getMatterId())))
                    .mapToDouble(inv -> inv.getAmount() != null ? inv.getAmount().doubleValue() : 0.0)
                    .sum();

            rows.add(List.of(adv, activeCount, disposed.size(), winRate + "%", String.format("INR %,.0f", totalRev)));
        }

        return ReportTable.builder()
                .title("Advocate Workload Comparison Report")
                .filterSummary(filterSummary)
                .headers(headers)
                .rows(rows)
                .build();
    }
}

package in.tubalaw.courtos.modules.reports.builder;

import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.reports.dto.ReportTable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CourtWiseReportBuilder {

    public ReportTable build(List<Matter> filteredMatters, String filterSummary) {
        List<String> headers = List.of("Court Jurisdiction", "Total Matters", "Disposed Cases", "Won Cases", "Win Rate (%)", "Compliance Rating");
        List<List<Object>> rows = new ArrayList<>();

        Map<String, List<Matter>> groupedByCourt = filteredMatters.stream()
                .filter(m -> m.getCourt() != null && !m.getCourt().isBlank())
                .collect(Collectors.groupingBy(Matter::getCourt));

        for (Map.Entry<String, List<Matter>> entry : groupedByCourt.entrySet()) {
            String court = entry.getKey();
            List<Matter> courtMatters = entry.getValue();

            long total = courtMatters.size();
            List<Matter> disposed = courtMatters.stream().filter(m -> "Disposed".equalsIgnoreCase(m.getStatus())).toList();
            long won = disposed.stream().filter(m -> "Won".equalsIgnoreCase(m.getOutcome())).count();
            long lost = disposed.stream().filter(m -> "Lost".equalsIgnoreCase(m.getOutcome())).count();

            int winRate = (won + lost > 0) ? (int) Math.round((double) won / (won + lost) * 100) : 0;
            String rating = winRate >= 80 ? "Outstanding" : winRate >= 50 ? "Excellent" : "In Progress";

            rows.add(List.of(court, total, disposed.size(), won, winRate + "%", rating));
        }

        return ReportTable.builder()
                .title("Court-wise Performance Report")
                .filterSummary(filterSummary)
                .headers(headers)
                .rows(rows)
                .build();
    }
}

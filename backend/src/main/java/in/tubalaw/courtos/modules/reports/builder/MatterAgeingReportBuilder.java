package in.tubalaw.courtos.modules.reports.builder;

import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.reports.dto.ReportTable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class MatterAgeingReportBuilder {

    public ReportTable build(List<Matter> filteredMatters, String filterSummary) {
        List<String> headers = List.of("Case No", "Matter Title", "Filing Date", "Ageing (Days)", "Age Bucket", "Current Stage", "Limitation Deadline");
        List<List<Object>> rows = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (Matter m : filteredMatters) {
            String caseNo = m.getCaseNo() != null ? m.getCaseNo() : "—";
            String title = m.getTitle();
            LocalDate fd = m.getFilingDate();
            if (fd == null && m.getCreatedAt() != null) {
                fd = LocalDate.ofInstant(m.getCreatedAt(), java.time.ZoneId.systemDefault());
            }

            long daysPending = (fd != null) ? ChronoUnit.DAYS.between(fd, now) : 0;
            String bucket = daysPending <= 30 ? "0-30 Days" : daysPending <= 90 ? "31-90 Days" : daysPending <= 180 ? "91-180 Days" : "180+ Days";
            String stage = m.getStage() != null ? m.getStage() : "Pre-filing";
            String deadline = m.getLimitationDeadline() != null ? m.getLimitationDeadline().toString() : "N/A";

            rows.add(List.of(caseNo, title, fd != null ? fd.toString() : "—", daysPending + " days", bucket, stage, deadline));
        }

        return ReportTable.builder()
                .title("Matter Ageing & Deadlines Report")
                .filterSummary(filterSummary)
                .headers(headers)
                .rows(rows)
                .build();
    }
}

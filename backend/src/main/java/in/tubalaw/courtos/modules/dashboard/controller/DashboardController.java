package in.tubalaw.courtos.modules.dashboard.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.causelist.repository.HearingRepository;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.tasks.repository.TaskRepository;
import in.tubalaw.courtos.modules.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MatterRepository   matterRepo;
    private final HearingRepository  hearingRepo;
    private final TaskRepository     taskRepo;
    private final InvoiceRepository  invoiceRepo;

    private static final String TENANT = "default";

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeMatters",    matterRepo.countByTenantIdAndStatus(TENANT, "Active"));
        stats.put("closedThisYear",   matterRepo.countByTenantIdAndStatus(TENANT, "Closed"));
        stats.put("hearingsToday",    hearingRepo.countByTenantIdAndHearingDate(TENANT, LocalDate.now()));
        stats.put("hearingsThisWeek", hearingRepo.countByTenantIdAndHearingDate(TENANT, LocalDate.now())); // simplified
        stats.put("openTasks",        taskRepo.countByTenantIdAndDone(TENANT, false));
        stats.put("urgentHearings",   hearingRepo.countByTenantIdAndStatus(TENANT, "Urgent"));
        stats.put("pendingFilings",   0L); // from filings repo
        stats.put("totalOutstanding", invoiceRepo.sumOutstanding(TENANT));
        stats.put("totalCollected",   invoiceRepo.sumCollected(TENANT));
        stats.put("overdueInvoices",  invoiceRepo.countByTenantIdAndStatus(TENANT, "Overdue"));
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> timeline() {
        var hearings = hearingRepo.findByDate(TENANT, LocalDate.now());
        var items = hearings.stream().<Map<String, Object>>map(h -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date", h.getHearingDate().toString());
            m.put("title", h.getCaseTitle());
            m.put("sub", h.getCourt() + (h.getBench() != null ? " · " + h.getBench() : ""));
            m.put("urgent", "Urgent".equals(h.getStatus()));
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/team-performance")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> teamPerformance() {
        List<Map<String, Object>> perf = List.of(
            Map.of("name","Adv. Amit Sharma","tasksOpen",8,"tasksTotal",15,"percentage",53,"role","admin"),
            Map.of("name","Adv. Priya Kapoor","tasksOpen",5,"tasksTotal",12,"percentage",42,"role","senior"),
            Map.of("name","Ravi Mehta","tasksOpen",3,"tasksTotal",8,"percentage",38,"role","clerk")
        );
        return ResponseEntity.ok(ApiResponse.ok(perf));
    }

    @GetMapping("/court-distribution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> courtDistribution() {
        var matters = matterRepo.findAllByTenantId(TENANT);
        Map<String, Long> byCount = new HashMap<>();
        matters.forEach(m -> byCount.merge(m.getCourt() != null ? m.getCourt() : "Unknown", 1L, Long::sum));
        var list = byCount.entrySet().stream()
            .<Map<String, Object>>map(e -> Map.of("court", e.getKey(), "count", e.getValue(), "pct", 0))
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenue() {
        double collected = invoiceRepo.sumCollected(TENANT);
        double outstanding = invoiceRepo.sumOutstanding(TENANT);
        Map<String, Object> rev = new HashMap<>();
        rev.put("month", "Jul");
        rev.put("year", 2026);
        rev.put("collected", collected);
        rev.put("outstanding", outstanding);
        rev.put("overdue", invoiceRepo.sumOverdue(TENANT));
        rev.put("weeklyData", List.of(
            Map.of("label","Mon","amount",12000),
            Map.of("label","Tue","amount",18000),
            Map.of("label","Wed","amount",8000),
            Map.of("label","Thu","amount",15000),
            Map.of("label","Fri","amount",22000),
            Map.of("label","Sat","amount",3000),
            Map.of("label","Sun","amount",1500)
        ));
        return ResponseEntity.ok(ApiResponse.ok(rev));
    }
}

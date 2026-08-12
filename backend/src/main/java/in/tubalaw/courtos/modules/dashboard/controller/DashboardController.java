package in.tubalaw.courtos.modules.dashboard.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.causelist.repository.HearingRepository;
import in.tubalaw.courtos.modules.diary.repository.DiaryEventRepository;
import in.tubalaw.courtos.modules.filings.repository.FilingRepository;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.tasks.repository.TaskRepository;
import in.tubalaw.courtos.modules.billing.repository.InvoiceRepository;
import in.tubalaw.courtos.modules.auth.repository.UserRepository;
import in.tubalaw.courtos.modules.masters.repository.MasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MatterRepository     matterRepo;
    private final HearingRepository    hearingRepo;
    private final TaskRepository       taskRepo;
    private final InvoiceRepository    invoiceRepo;
    private final FilingRepository     filingRepo;
    private final DiaryEventRepository diaryEventRepo;
    private final UserRepository       userRepo;
    private final MasterRepository     masterRepo;

    private static final String TENANT = "default";

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        long hearingsThisWeekCount = hearingRepo.countByTenantIdAndHearingDateBetween(TENANT, startOfWeek, endOfWeek);
        long diaryEventsThisWeekCount = diaryEventRepo.countByTenantIdAndEventDateBetween(TENANT, startOfWeek, endOfWeek);

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeMatters",    matterRepo.countByTenantIdAndStatus(TENANT, "Active"));
        stats.put("closedThisYear",   matterRepo.countClosedOrDisposed(TENANT));
        stats.put("hearingsToday",    hearingRepo.countByTenantIdAndHearingDate(TENANT, today));
        stats.put("hearingsThisWeek", hearingsThisWeekCount + diaryEventsThisWeekCount);
        stats.put("openTasks",        taskRepo.countByTenantIdAndDone(TENANT, false));
        stats.put("urgentHearings",   hearingRepo.countByTenantIdAndStatus(TENANT, "Urgent"));
        stats.put("pendingFilings",   filingRepo.countByTenantIdAndStatusNot(TENANT, "Filed"));
        stats.put("totalOutstanding", invoiceRepo.sumOutstanding(TENANT));
        stats.put("totalCollected",   invoiceRepo.sumCollected(TENANT));
        stats.put("overdueInvoices",  invoiceRepo.countByTenantIdAndStatus(TENANT, "Overdue"));
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> timeline() {
        LocalDate today = LocalDate.now();
        var hearings = hearingRepo.findByDate(TENANT, today);
        var diaryEvents = diaryEventRepo.findByTenantIdAndEventDate(TENANT, today);
        var tasks = taskRepo.findAllByTenantId(TENANT);
        var filings = filingRepo.findAllByTenantId(TENANT);

        List<Map<String, Object>> items = new ArrayList<>();

        for (var h : hearings) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", h.getHearingDate().toString());
            m.put("title", h.getCaseTitle() != null ? "Court Hearing: " + h.getCaseTitle() : "Court Hearing");
            m.put("sub", (h.getCourt() != null ? h.getCourt() : "Court") + (h.getBench() != null ? " · " + h.getBench() : ""));
            m.put("urgent", "Urgent".equalsIgnoreCase(h.getStatus()));
            m.put("time", h.getHearingTime() != null ? h.getHearingTime() : "09:30 AM");
            items.add(m);
        }

        for (var d : diaryEvents) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", d.getEventDate().toString());
            m.put("title", d.getTitle());
            m.put("sub", (d.getCourt() != null && !d.getCourt().isBlank() ? d.getCourt() : "Diary Appointment") + (d.getMatterTitle() != null ? " · " + d.getMatterTitle() : ""));
            m.put("urgent", d.isUrgent());
            m.put("time", d.getEventTime() != null ? d.getEventTime() : "10:00 AM");
            items.add(m);
        }

        for (var t : tasks) {
            if (!t.isDone() && t.getDueDate() != null && today.equals(t.getDueDate())) {
                Map<String, Object> m = new HashMap<>();
                m.put("date", t.getDueDate().toString());
                m.put("title", "Task: " + t.getTitle());
                m.put("sub", (t.getMatterTitle() != null ? t.getMatterTitle() : "Task") + (t.getAssignedTo() != null ? " · " + t.getAssignedTo() : ""));
                m.put("urgent", "Urgent".equalsIgnoreCase(t.getPriority()) || "High".equalsIgnoreCase(t.getPriority()));
                m.put("time", "09:00 AM");
                items.add(m);
            }
        }

        for (var f : filings) {
            if (!"Filed".equalsIgnoreCase(f.getStatus()) && f.getDueDate() != null && today.equals(f.getDueDate())) {
                Map<String, Object> m = new HashMap<>();
                m.put("date", f.getDueDate().toString());
                m.put("title", "Filing Due: " + f.getTitle());
                m.put("sub", (f.getCourt() != null ? f.getCourt() : "Court Filing") + (f.getMatterTitle() != null ? " · " + f.getMatterTitle() : ""));
                m.put("urgent", true);
                m.put("time", "05:00 PM");
                items.add(m);
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/team-performance")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> teamPerformance() {
        // Single aggregation query — replaces the N+1 per-user task fetch loop
        var summaries = taskRepo.summarizeByUser(TENANT);
        List<Map<String, Object>> perf = new ArrayList<>();

        for (var s : summaries) {
            String name = s.getName();
            if (name == null || name.isBlank()) continue;
            long total = s.getTotal();
            long done  = s.getDone();
            long open  = total - done;
            int pct = total > 0 ? (int) Math.round(((double) done / total) * 100) : 100;

            // Resolve role from user table (one lookup per unique name — acceptable)
            String role = userRepo.findAllByTenantId(TENANT).stream()
                    .filter(u -> name.equalsIgnoreCase(u.getName()))
                    .map(u -> u.getRole() != null ? u.getRole().toLowerCase() : "advocate")
                    .findFirst().orElse("advocate");

            Map<String, Object> m = new HashMap<>();
            m.put("name", name);
            m.put("tasksOpen", open);
            m.put("tasksTotal", total);
            m.put("percentage", pct);
            m.put("role", role);
            perf.add(m);
        }

        if (perf.isEmpty()) {
            perf = List.of(
                Map.of("name","Adv. Amit Sharma","tasksOpen",0,"tasksTotal",0,"percentage",100,"role","admin"),
                Map.of("name","Adv. Priya Kapoor","tasksOpen",0,"tasksTotal",0,"percentage",100,"role","senior")
            );
        }

        return ResponseEntity.ok(ApiResponse.ok(perf));
    }

    @GetMapping("/court-distribution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> courtDistribution() {
        // Single aggregation query — replaces the former findAllByTenantId() + Java groupBy full-scan
        var aggregates = matterRepo.countByCourt(TENANT);
        long total = aggregates.stream().mapToLong(MatterRepository.CourtCount::getCount).sum();

        // Build a map for sorting reference
        Map<String, Long> byCount = new LinkedHashMap<>();
        aggregates.forEach(a -> byCount.put(
                (a.getCourt() != null && !a.getCourt().isBlank()) ? a.getCourt() : "Unassigned",
                a.getCount()));

        // Get courts ordering from master
        List<String> courtOrder = masterRepo.findByTenantIdAndCategory(TENANT, "courts")
                .map(m -> m.getItems() != null ? Arrays.stream(m.getItems()).map(String::trim).toList() : List.<String>of())
                .orElse(List.of());

        var list = byCount.entrySet().stream()
            .sorted((e1, e2) -> {
                String c1 = e1.getKey() != null ? e1.getKey().trim() : "";
                String c2 = e2.getKey() != null ? e2.getKey().trim() : "";

                int idx1 = -1, idx2 = -1;
                for (int i = 0; i < courtOrder.size(); i++) {
                    if (courtOrder.get(i).equalsIgnoreCase(c1)) { idx1 = i; break; }
                }
                for (int i = 0; i < courtOrder.size(); i++) {
                    if (courtOrder.get(i).equalsIgnoreCase(c2)) { idx2 = i; break; }
                }
                if (idx1 != -1 && idx2 != -1) return Integer.compare(idx1, idx2);
                if (idx1 != -1) return -1;
                if (idx2 != -1) return 1;
                return c1.compareToIgnoreCase(c2);
            })
            .<Map<String, Object>>map(e -> {
                int pct = total > 0 ? (int) Math.round((double) e.getValue() / total * 100) : 0;
                Map<String, Object> m = new HashMap<>();
                m.put("court", e.getKey());
                m.put("count", e.getValue());
                m.put("pct", pct);
                return m;
            })
            .toList();

        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenue() {
        double collected = invoiceRepo.sumCollected(TENANT);
        double outstanding = invoiceRepo.sumOutstanding(TENANT);
        double overdue = invoiceRepo.sumOverdue(TENANT);
        LocalDate now = LocalDate.now();

        Map<String, Object> rev = new HashMap<>();
        rev.put("month", now.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        rev.put("year", now.getYear());
        rev.put("collected", collected);
        rev.put("outstanding", outstanding);
        rev.put("overdue", overdue);
        rev.put("weeklyData", List.of(
            Map.of("label","Mon","amount", collected > 0 ? Math.round(collected * 0.15) : 0),
            Map.of("label","Tue","amount", collected > 0 ? Math.round(collected * 0.25) : 0),
            Map.of("label","Wed","amount", collected > 0 ? Math.round(collected * 0.10) : 0),
            Map.of("label","Thu","amount", collected > 0 ? Math.round(collected * 0.20) : 0),
            Map.of("label","Fri","amount", collected > 0 ? Math.round(collected * 0.20) : 0),
            Map.of("label","Sat","amount", collected > 0 ? Math.round(collected * 0.07) : 0),
            Map.of("label","Sun","amount", collected > 0 ? Math.round(collected * 0.03) : 0)
        ));
        return ResponseEntity.ok(ApiResponse.ok(rev));
    }
}

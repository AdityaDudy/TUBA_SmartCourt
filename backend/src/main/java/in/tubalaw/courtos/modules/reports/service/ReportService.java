package in.tubalaw.courtos.modules.reports.service;

import in.tubalaw.courtos.modules.billing.entity.Invoice;
import in.tubalaw.courtos.modules.billing.repository.InvoiceRepository;
import in.tubalaw.courtos.modules.causelist.entity.Hearing;
import in.tubalaw.courtos.modules.causelist.repository.HearingRepository;
import in.tubalaw.courtos.modules.clients.entity.Client;
import in.tubalaw.courtos.modules.clients.repository.ClientRepository;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.reports.entity.ReportGeneration;
import in.tubalaw.courtos.modules.reports.entity.ReportSchedule;
import in.tubalaw.courtos.modules.reports.repository.ReportGenerationRepository;
import in.tubalaw.courtos.modules.reports.repository.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MatterRepository matterRepo;
    private final InvoiceRepository invoiceRepo;
    private final ClientRepository clientRepo;
    private final HearingRepository hearingRepo;
    private final ReportGenerationRepository generationRepo;
    private final ReportScheduleRepository scheduleRepo;

    private static final String TENANT = "default";

    public Map<String, Object> getSummary(String fy, String practiceArea, String court, String advocate) {
        // Load all data for this tenant
        List<Matter> matters = matterRepo.findAllByTenantId(TENANT);
        List<Invoice> invoices = invoiceRepo.findAllByTenantId(TENANT);
        List<Hearing> hearings = hearingRepo.findAllByTenantId(TENANT);
        List<Client> clients = clientRepo.findAllByTenantId(TENANT);

        // Date range calculation for Indian Financial Year (e.g. FY 2026 starts on 2026-04-01 to 2027-03-31)
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (fy != null && !fy.trim().isEmpty()) {
            try {
                int year = Integer.parseInt(fy.trim());
                startDate = LocalDate.of(year, 4, 1);
                endDate = LocalDate.of(year + 1, 3, 31);
            } catch (NumberFormatException ignored) {}
        }

        final LocalDate finalStart = startDate;
        final LocalDate finalEnd = endDate;

        // ── 1. Filter Matters ───────────────────────────────────────
        List<Matter> filteredMatters = matters.stream()
            .filter(m -> {
                if (finalStart != null && finalEnd != null) {
                    LocalDate fd = m.getFilingDate();
                    if (fd == null && m.getCreatedAt() != null) {
                        fd = LocalDate.ofInstant(m.getCreatedAt(), java.time.ZoneId.systemDefault());
                    }
                    if (fd == null) return false;
                    return !fd.isBefore(finalStart) && !fd.isAfter(finalEnd);
                }
                return true;
            })
            .filter(m -> practiceArea == null || practiceArea.trim().isEmpty() || 
                         (m.getArea() != null && m.getArea().equalsIgnoreCase(practiceArea.trim())))
            .filter(m -> court == null || court.trim().isEmpty() || 
                         (m.getCourt() != null && m.getCourt().equalsIgnoreCase(court.trim())))
            .filter(m -> advocate == null || advocate.trim().isEmpty() || 
                         (m.getAdvocate() != null && m.getAdvocate().equalsIgnoreCase(advocate.trim())))
            .collect(Collectors.toList());

        // ── 2. Calculate win rate ──────────────────────────────────
        // Win rate = Won / (Won + Lost)
        List<Matter> disposedMatters = filteredMatters.stream()
            .filter(m -> "Disposed".equalsIgnoreCase(m.getStatus()))
            .collect(Collectors.toList());
        long wonCount = disposedMatters.stream().filter(m -> "Won".equalsIgnoreCase(m.getOutcome())).count();
        long lostCount = disposedMatters.stream().filter(m -> "Lost".equalsIgnoreCase(m.getOutcome())).count();
        int winRate = 0;
        if (wonCount + lostCount > 0) {
            winRate = (int) Math.round((double) wonCount / (wonCount + lostCount) * 100);
        }

        // ── 3. Filter Invoices & Calculate Revenue ─────────────────
        // Invoices filter based on matching matter advocate, court, practiceArea, and FY date range
        List<Invoice> filteredInvoices = invoices.stream()
            .filter(inv -> {
                if (finalStart != null && finalEnd != null) {
                    if (inv.getDueDate() == null) return false;
                    LocalDate dd = inv.getDueDate();
                    return !dd.isBefore(finalStart) && !dd.isAfter(finalEnd);
                }
                return true;
            })
            .filter(inv -> {
                // Link with matters to filter by area, court, advocate
                if (inv.getMatterId() == null) return true; // keep general invoices
                Optional<Matter> oMat = matters.stream().filter(m -> m.getId().equals(inv.getMatterId())).findFirst();
                if (oMat.isPresent()) {
                    Matter m = oMat.get();
                    if (practiceArea != null && !practiceArea.trim().isEmpty() && 
                        (m.getArea() == null || !m.getArea().equalsIgnoreCase(practiceArea.trim()))) return false;
                    if (court != null && !court.trim().isEmpty() && 
                        (m.getCourt() == null || !m.getCourt().equalsIgnoreCase(court.trim()))) return false;
                    if (advocate != null && !advocate.trim().isEmpty() && 
                        (m.getAdvocate() == null || !m.getAdvocate().equalsIgnoreCase(advocate.trim()))) return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        double collectedRevenue = filteredInvoices.stream()
            .filter(inv -> "Paid".equalsIgnoreCase(inv.getStatus()) || "Partially Paid".equalsIgnoreCase(inv.getStatus()))
            .mapToDouble(inv -> inv.getPaidAmount() != null ? inv.getPaidAmount().doubleValue() : 0.0)
            .sum();

        // ── 4. Clients Count ────────────────────────────────────────
        // Clients count can be all active clients, or clients associated with filtered matters
        long activeClientsCount = clients.stream()
            .filter(c -> {
                // associate with filtered matters
                return filteredMatters.stream().anyMatch(m -> m.getClientId() != null && m.getClientId().equals(c.getId()));
            })
            .count();
        if (activeClientsCount == 0 && (practiceArea == null || practiceArea.trim().isEmpty()) && (court == null || court.trim().isEmpty()) && (advocate == null || advocate.trim().isEmpty())) {
            activeClientsCount = clients.stream().filter(c -> "Active".equalsIgnoreCase(c.getStatus())).count();
        }

        // ── 5. Matters By Stage Funnel ──────────────────────────────
        Map<String, Long> stageCounts = filteredMatters.stream()
            .filter(m -> m.getStage() != null && !m.getStage().trim().isEmpty())
            .collect(Collectors.groupingBy(Matter::getStage, Collectors.counting()));
        List<Map<String, Object>> funnel = stageCounts.entrySet().stream()
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("stage", e.getKey());
                map.put("count", e.getValue());
                return map;
            })
            .sorted((a, b) -> Long.compare((long) b.get("count"), (long) a.get("count")))
            .collect(Collectors.toList());

        // ── 6. Matters By Practice Area Donut ────────────────────────
        Map<String, Long> areaCounts = filteredMatters.stream()
            .filter(m -> m.getArea() != null && !m.getArea().trim().isEmpty())
            .collect(Collectors.groupingBy(Matter::getArea, Collectors.counting()));
        List<Map<String, Object>> donut = areaCounts.entrySet().stream()
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("area", e.getKey());
                map.put("count", e.getValue());
                return map;
            })
            .collect(Collectors.toList());

        // ── 7. Filter Hearings & Build Monthly/Quarterly Datasets ────
        List<Hearing> filteredHearings = hearings.stream()
            .filter(h -> {
                if (finalStart != null && finalEnd != null) {
                    if (h.getHearingDate() == null) return false;
                    return !h.getHearingDate().isBefore(finalStart) && !h.getHearingDate().isAfter(finalEnd);
                }
                return true;
            })
            .filter(h -> {
                if (h.getMatterId() == null) return true;
                Optional<Matter> oMat = matters.stream().filter(m -> m.getId().equals(h.getMatterId())).findFirst();
                if (oMat.isPresent()) {
                    Matter m = oMat.get();
                    if (practiceArea != null && !practiceArea.trim().isEmpty() && 
                        (m.getArea() == null || !m.getArea().equalsIgnoreCase(practiceArea.trim()))) return false;
                    if (court != null && !court.trim().isEmpty() && 
                        (m.getCourt() == null || !m.getCourt().equalsIgnoreCase(court.trim()))) return false;
                    if (advocate != null && !advocate.trim().isEmpty() && 
                        (m.getAdvocate() == null || !m.getAdvocate().equalsIgnoreCase(advocate.trim()))) return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        // Monthly hearings count (Jan to Dec)
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int[] monthlyCounts = new int[12];
        for (Hearing h : filteredHearings) {
            if (h.getHearingDate() != null) {
                int monthIdx = h.getHearingDate().getMonthValue() - 1;
                monthlyCounts[monthIdx]++;
            }
        }
        List<Map<String, Object>> monthlyHearings = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("label", months[i]);
            map.put("count", monthlyCounts[i]);
            monthlyHearings.add(map);
        }

        // Quarterly hearings count
        String[] quarters = {"Q1 (AMJ)", "Q2 (JAS)", "Q3 (OND)", "Q4 (JFM)"};
        int[] quarterlyCounts = new int[4];
        for (Hearing h : filteredHearings) {
            if (h.getHearingDate() != null) {
                int mVal = h.getHearingDate().getMonthValue();
                if (mVal >= 4 && mVal <= 6) quarterlyCounts[0]++;      // AMJ
                else if (mVal >= 7 && mVal <= 9) quarterlyCounts[1]++; // JAS
                else if (mVal >= 10 && mVal <= 12) quarterlyCounts[2]++; // OND
                else quarterlyCounts[3]++; // JFM
            }
        }
        List<Map<String, Object>> quarterlyHearings = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("label", quarters[i]);
            map.put("count", quarterlyCounts[i]);
            quarterlyHearings.add(map);
        }

        // ── 8. Advocate Performance Comparison ───────────────────────
        // Group matters by advocate
        Map<String, List<Matter>> mattersByAdvocate = filteredMatters.stream()
            .filter(m -> m.getAdvocate() != null && !m.getAdvocate().trim().isEmpty())
            .collect(Collectors.groupingBy(Matter::getAdvocate));

        List<Map<String, Object>> advocatePerformance = mattersByAdvocate.entrySet().stream()
            .map(e -> {
                String advName = e.getKey();
                List<Matter> advMatters = e.getValue();

                // Win rate for this advocate
                List<Matter> advDisposed = advMatters.stream()
                    .filter(m -> "Disposed".equalsIgnoreCase(m.getStatus()))
                    .collect(Collectors.toList());
                long advWon = advDisposed.stream().filter(m -> "Won".equalsIgnoreCase(m.getOutcome())).count();
                long advLost = advDisposed.stream().filter(m -> "Lost".equalsIgnoreCase(m.getOutcome())).count();
                int advWinRate = 0;
                if (advWon + advLost > 0) {
                    advWinRate = (int) Math.round((double) advWon / (advWon + advLost) * 100);
                }

                double advRevenue = filteredInvoices.stream()
                    .filter(inv -> {
                        if (inv.getMatterId() == null) return false;
                        Optional<Matter> oM = advMatters.stream().filter(m -> m.getId().equals(inv.getMatterId())).findFirst();
                        return oM.isPresent();
                    })
                    .filter(inv -> "Paid".equalsIgnoreCase(inv.getStatus()) || "Partially Paid".equalsIgnoreCase(inv.getStatus()))
                    .mapToDouble(inv -> inv.getPaidAmount() != null ? inv.getPaidAmount().doubleValue() : 0.0)
                    .sum();

                Map<String, Object> map = new HashMap<>();
                map.put("advocate", advName);
                map.put("mattersCount", advMatters.size());
                map.put("winRate", advWinRate);
                map.put("revenue", advRevenue);
                return map;
            })
            .collect(Collectors.toList());

        // Assemble result
        Map<String, Object> result = new HashMap<>();
        result.put("totalMatters", filteredMatters.size());
        result.put("winRate", winRate);
        result.put("revenue", collectedRevenue);
        result.put("clients", activeClientsCount);
        result.put("funnel", funnel);
        result.put("donut", donut);
        result.put("monthlyHearings", monthlyHearings);
        result.put("quarterlyHearings", quarterlyHearings);
        result.put("advocatePerformance", advocatePerformance);

        return result;
    }

    private final in.tubalaw.courtos.modules.reports.builder.CourtWiseReportBuilder courtWiseReportBuilder;
    private final in.tubalaw.courtos.modules.reports.builder.AdvocateWorkloadReportBuilder advocateWorkloadReportBuilder;
    private final in.tubalaw.courtos.modules.reports.builder.RevenueReportBuilder revenueReportBuilder;
    private final in.tubalaw.courtos.modules.reports.builder.MatterAgeingReportBuilder matterAgeingReportBuilder;
    private final in.tubalaw.courtos.modules.reports.renderer.ExcelReportRenderer excelReportRenderer;
    private final in.tubalaw.courtos.modules.reports.renderer.PdfReportRenderer pdfReportRenderer;

    public in.tubalaw.courtos.modules.reports.dto.ReportTable buildReportTable(String type, String fy, String practiceArea, String court, String advocate) {
        List<Matter> matters = matterRepo.findAllByTenantId(TENANT);
        List<Invoice> invoices = invoiceRepo.findAllByTenantId(TENANT);

        // Date range calculation for Indian Financial Year
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (fy != null && !fy.trim().isEmpty()) {
            try {
                int year = Integer.parseInt(fy.trim());
                startDate = LocalDate.of(year, 4, 1);
                endDate = LocalDate.of(year + 1, 3, 31);
            } catch (NumberFormatException ignored) {}
        }

        final LocalDate finalStart = startDate;
        final LocalDate finalEnd = endDate;

        List<Matter> filteredMatters = matters.stream()
            .filter(m -> {
                if (finalStart != null && finalEnd != null) {
                    LocalDate fd = m.getFilingDate();
                    if (fd == null && m.getCreatedAt() != null) {
                        fd = LocalDate.ofInstant(m.getCreatedAt(), java.time.ZoneId.systemDefault());
                    }
                    if (fd == null) return false;
                    return !fd.isBefore(finalStart) && !fd.isAfter(finalEnd);
                }
                return true;
            })
            .filter(m -> practiceArea == null || practiceArea.trim().isEmpty() || (m.getArea() != null && m.getArea().equalsIgnoreCase(practiceArea.trim())))
            .filter(m -> court == null || court.trim().isEmpty() || (m.getCourt() != null && m.getCourt().equalsIgnoreCase(court.trim())))
            .filter(m -> advocate == null || advocate.trim().isEmpty() || (m.getAdvocate() != null && m.getAdvocate().equalsIgnoreCase(advocate.trim())))
            .collect(Collectors.toList());

        List<Invoice> filteredInvoices = invoices.stream()
            .filter(inv -> {
                if (finalStart != null && finalEnd != null) {
                    if (inv.getDueDate() == null) return false;
                    LocalDate dd = inv.getDueDate();
                    return !dd.isBefore(finalStart) && !dd.isAfter(finalEnd);
                }
                return true;
            })
            .collect(Collectors.toList());

        String filterSummary = "FY: " + (fy != null && !fy.isBlank() ? fy : "All") + 
                " | Area: " + (practiceArea != null && !practiceArea.isBlank() ? practiceArea : "All") + 
                " | Court: " + (court != null && !court.isBlank() ? court : "All") + 
                " | Advocate: " + (advocate != null && !advocate.isBlank() ? advocate : "All");

        String typeLower = type != null ? type.toLowerCase() : "";
        if (typeLower.contains("court")) {
            return courtWiseReportBuilder.build(filteredMatters, filterSummary);
        } else if (typeLower.contains("advocate")) {
            return advocateWorkloadReportBuilder.build(filteredMatters, filteredInvoices, filterSummary);
        } else if (typeLower.contains("revenue") || typeLower.contains("invoice")) {
            return revenueReportBuilder.build(filteredInvoices, filterSummary);
        } else {
            return matterAgeingReportBuilder.build(filteredMatters, filterSummary);
        }
    }

    public byte[] generateReportBytes(String type, String format, String fy, String practiceArea, String court, String advocate) throws Exception {
        in.tubalaw.courtos.modules.reports.dto.ReportTable reportTable = buildReportTable(type, fy, practiceArea, court, advocate);
        if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format) || "csv".equalsIgnoreCase(format)) {
            return excelReportRenderer.render(reportTable);
        } else {
            return pdfReportRenderer.render(reportTable);
        }
    }

    public void logGeneration(String type, String filters, String format) {
        ReportGeneration gen = ReportGeneration.builder()
            .reportType(type)
            .filtersUsed(filters)
            .generatedBy("Adv. Amit Sharma")
            .generatedAt(LocalDateTime.now())
            .format(format)
            .build();
        generationRepo.save(gen);
    }

    public ReportSchedule scheduleReport(String type, String filters, String frequency, String email) {
        ReportSchedule sched = ReportSchedule.builder()
            .reportType(type)
            .filtersUsed(filters)
            .frequency(frequency)
            .emailRecipient(email)
            .createdAt(LocalDateTime.now())
            .build();
        return scheduleRepo.save(sched);
    }
}

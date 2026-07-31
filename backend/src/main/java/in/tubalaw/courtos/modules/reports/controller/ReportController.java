package in.tubalaw.courtos.modules.reports.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.reports.entity.ReportSchedule;
import in.tubalaw.courtos.modules.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(
            @RequestParam(required = false) String fy,
            @RequestParam(required = false) String practiceArea,
            @RequestParam(required = false) String court,
            @RequestParam(required = false) String advocate) {
        
        Map<String, Object> data = reportService.getSummary(fy, practiceArea, court, advocate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody Map<String, String> payload) {
        String type = payload.getOrDefault("type", "General Summary");
        String format = payload.getOrDefault("format", "pdf");
        String filters = payload.getOrDefault("filters", "None");

        // Log manual export generation
        reportService.logGeneration(type, filters, format);

        String fyVal = "";
        String areaVal = "";
        String courtVal = "";
        String advocateVal = "";
        if (filters != null) {
            String[] parts = filters.split("\\|");
            for (String part : parts) {
                if (part.contains("FY:")) {
                    fyVal = part.substring(part.indexOf("FY:") + 3).trim();
                    if ("All".equalsIgnoreCase(fyVal)) fyVal = "";
                } else if (part.contains("Area:")) {
                    areaVal = part.substring(part.indexOf("Area:") + 5).trim();
                    if ("All".equalsIgnoreCase(areaVal)) areaVal = "";
                } else if (part.contains("Court:")) {
                    courtVal = part.substring(part.indexOf("Court:") + 6).trim();
                    if ("All".equalsIgnoreCase(courtVal)) courtVal = "";
                } else if (part.contains("Advocate:")) {
                    advocateVal = part.substring(part.indexOf("Advocate:") + 9).trim();
                    if ("All".equalsIgnoreCase(advocateVal)) advocateVal = "";
                }
            }
        }

        try {
            byte[] content = reportService.generateReportBytes(type, format, fyVal, areaVal, courtVal, advocateVal);
            
            boolean isExcel = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format) || "csv".equalsIgnoreCase(format);
            String extension = isExcel ? "xlsx" : "pdf";
            String filename = type.toLowerCase().replaceAll("[^a-z0-9]", "_") + "." + extension;

            org.springframework.http.MediaType mediaType = isExcel 
                ? org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : org.springframework.http.MediaType.APPLICATION_PDF;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<ReportSchedule>> schedule(@RequestBody Map<String, String> payload) {
        String type = payload.get("type");
        String filters = payload.getOrDefault("filters", "None");
        String frequency = payload.getOrDefault("frequency", "Monthly");
        String email = payload.get("email");

        ReportSchedule schedule = reportService.scheduleReport(type, filters, frequency, email);
        return ResponseEntity.ok(ApiResponse.ok(schedule));
    }
}

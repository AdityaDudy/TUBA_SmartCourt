package in.tubalaw.courtos.modules.settings.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.settings.entity.SystemSetting;
import in.tubalaw.courtos.modules.settings.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final AuditLogService auditLogService;
    private final SystemSettingRepository systemSettingRepository;
    private static final String TENANT = "default";

    private String getSetting(String key, String defaultValue) {
        return systemSettingRepository.findById(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    private void saveSetting(String key, Object value) {
        systemSettingRepository.save(SystemSetting.builder()
                .key(key)
                .value(value != null ? value.toString() : "")
                .build());
    }

    @GetMapping("/firm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFirm() {
        Map<String, Object> settings = Map.of(
            "firmName", getSetting("firmName", "TUBA Law Associates"),
            "firmEmail", getSetting("firmEmail", "info@tubalaw.com"),
            "firmPhone", getSetting("firmPhone", "+91-11-4567-8900"),
            "firmAddress", getSetting("firmAddress", "New Delhi, India"),
            "currency", getSetting("currency", "INR"),
            "timezone", getSetting("timezone", "Asia/Kolkata"),
            "dateFormat", getSetting("dateFormat", "DD/MM/YYYY")
        );
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @PutMapping("/firm")
    public ResponseEntity<ApiResponse<Void>> updateFirm(@RequestBody Map<String, Object> body) {
        body.forEach(this::saveSetting);
        return ResponseEntity.ok(ApiResponse.ok(null, "Firm settings saved."));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getNotif() {
        Map<String, Boolean> settings = Map.of(
            "hearingReminders", Boolean.parseBoolean(getSetting("hearingReminders", "true")),
            "taskReminders", Boolean.parseBoolean(getSetting("taskReminders", "true")),
            "emailAlerts", Boolean.parseBoolean(getSetting("emailAlerts", "true")),
            "whatsappAlerts", Boolean.parseBoolean(getSetting("whatsappAlerts", "false")),
            "overdueAlerts", Boolean.parseBoolean(getSetting("overdueAlerts", "true"))
        );
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<Void>> updateNotif(@RequestBody Map<String, Boolean> body) {
        body.forEach(this::saveSetting);
        return ResponseEntity.ok(ApiResponse.ok(null, "Notification preferences saved."));
    }

    @GetMapping("/security")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurity() {
        Map<String, Object> settings = Map.of(
            "mfaEnabled", Boolean.parseBoolean(getSetting("mfaEnabled", "true")),
            "maxAttempts", Integer.parseInt(getSetting("maxAttempts", "5")),
            "sessionTimeout", getSetting("sessionTimeout", "1 hr")
        );
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    public long getSessionTimeoutSeconds() {
        String str = getSetting("sessionTimeout", "1 hr").toLowerCase().trim();
        if (str.contains("min")) {
            String num = str.replace("min", "").trim();
            return Long.parseLong(num) * 60;
        } else if (str.contains("hr")) {
            String num = str.replace("hr", "").trim();
            return Long.parseLong(num) * 3600;
        }
        return 3600;
    }

    @PutMapping("/security")
    public ResponseEntity<ApiResponse<Void>> updateSecurity(@RequestBody Map<String, Object> body) {
        body.forEach(this::saveSetting);
        auditLogService.log(TENANT, 0L, "admin@smartcourt.com", "Security Settings Updated", "Settings", null,
                "Updated security parameters: MFA enabled=" + body.get("mfaEnabled") + 
                ", Max Attempts=" + body.get("maxAttempts") + 
                ", Session Timeout=" + body.get("sessionTimeout"), null, "HIGH");
        return ResponseEntity.ok(ApiResponse.ok(null, "Security settings saved."));
    }
}

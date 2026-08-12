package in.tubalaw.courtos.modules.auth.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.auth.entity.IpWhitelistEntry;
import in.tubalaw.courtos.modules.auth.repository.IpWhitelistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/security/ip-whitelist")
@RequiredArgsConstructor
public class IpWhitelistController {

    private final IpWhitelistRepository ipWhitelistRepository;
    private final AuditLogService auditLogService;
    private static final String TENANT = "default";

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWhitelist() {
        List<IpWhitelistEntry> entries = ipWhitelistRepository.findAllByTenantId(TENANT);
        List<Map<String, Object>> response = entries.stream().map(entry -> Map.<String, Object>of(
                "ip", entry.getIpAddress(),
                "label", entry.getLabel() == null ? "" : entry.getLabel(),
                "blocked", entry.isBlocked())).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IpWhitelistEntry>> addEntry(@RequestBody Map<String, Object> req) {
        IpWhitelistEntry entry = IpWhitelistEntry.builder()
                .tenantId(TENANT)
                .ipAddress((String) req.get("ip"))
                .label((String) req.get("label"))
                .blocked(req.get("blocked") != null && (Boolean) req.get("blocked"))
                .build();
        IpWhitelistEntry saved = ipWhitelistRepository.save(entry);
        auditLogService.log(TENANT, 0L, "admin@smartcourt.com", "IP Rule Added", "Security", saved.getId().toString(),
                "Added rule: " + saved.getIpAddress() + " (" + saved.getLabel() + "), blocked=" + saved.isBlocked(),
                null, "MEDIUM");
        return ResponseEntity.ok(ApiResponse.ok(saved, "IP Whitelist entry added successfully"));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteEntryByIp(@RequestParam String ip) {
        ipWhitelistRepository.deleteByIpAddressAndTenantId(ip, TENANT);
        auditLogService.log(TENANT, 0L, "admin@smartcourt.com", "IP Rule Removed", "Security", null,
                "Removed IP rule for: " + ip, null, "MEDIUM");
        return ResponseEntity.ok(ApiResponse.ok(null, "IP Whitelist entry removed"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEntryById(@PathVariable Long id) {
        ipWhitelistRepository.deleteById(id);
        auditLogService.log(TENANT, 0L, "admin@smartcourt.com", "IP Rule Removed", "Security", id.toString(),
                "Removed IP rule by ID", null, "MEDIUM");
        return ResponseEntity.ok(ApiResponse.ok(null, "IP Whitelist entry removed"));
    }
}

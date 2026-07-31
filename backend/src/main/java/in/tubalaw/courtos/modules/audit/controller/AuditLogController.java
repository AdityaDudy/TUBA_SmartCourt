package in.tubalaw.courtos.modules.audit.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.common.sse.SsePublisherService;
import in.tubalaw.courtos.modules.audit.entity.AuditEntry;
import in.tubalaw.courtos.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository repo;
    private final SsePublisherService ssePublisherService;
    private static final String TENANT = "default";

    /**
     * SSE stream — public so the browser can subscribe without re-auth on
     * EventSource
     */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return ssePublisherService.registerAuditEmitter();
    }

    /** Internal test endpoint */
    @GetMapping("/test")
    public List<AuditEntry> test() {
        return repo.findTop100ByTenantIdOrderByCreatedAtDesc(TENANT);
    }

    /**
     * Audit log — returns top audit entries or paginated entries.
     * Requires view_org_audit, view_own_audit, view_audit, or ROLE_ADMIN
     * permission.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('view_org_audit', 'view_own_audit', 'view_audit', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditEntry>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        int safeSize = Math.min(size, 100);
        Page<AuditEntry> result = repo.findByTenantIdOrderByCreatedAtDesc(
                TENANT, PageRequest.of(page, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(result.getContent()));
    }
}

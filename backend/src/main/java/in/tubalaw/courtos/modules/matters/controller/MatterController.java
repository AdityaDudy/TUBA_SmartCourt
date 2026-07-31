package in.tubalaw.courtos.modules.matters.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.service.MatterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matters")
@RequiredArgsConstructor
public class MatterController {

    private final MatterService matterService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    @GetMapping
    @PreAuthorize("hasAnyAuthority('view_all', 'edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Matter>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(matterService.list(status, type)));
    }

    /** Returns matters that have no linked CNR in tracked_cases — for bulk-link onboarding. */
    @GetMapping("/unlinked")
    @PreAuthorize("hasAnyAuthority('view_all', 'edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Matter>>> unlinked() {
        return ResponseEntity.ok(ApiResponse.ok(matterService.findUnlinked()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('view_all', 'edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Matter>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(matterService.search(q)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('view_all', 'edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Matter>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(matterService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Matter>> create(@RequestBody Matter matter) {
        Matter saved = matterService.create(matter);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Matter Created", "Matter", saved.getId().toString(),
                "Created matter: " + saved.getTitle() + " (Case No: " + saved.getCaseNo() + ")",
                request.getRemoteAddr(), "LOW");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Matter registered successfully!"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Matter>> update(@PathVariable Long id, @RequestBody Matter matter) {
        Matter saved = matterService.update(id, matter);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Matter Updated", "Matter", id.toString(),
                "Updated matter: " + saved.getTitle(), request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(saved, "Matter updated."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('delete_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Matter matter = matterService.getById(id);
        matterService.delete(id);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Matter Deleted", "Matter", id.toString(),
                "Deleted matter: " + matter.getTitle(), request.getRemoteAddr(), "HIGH");
        return ResponseEntity.ok(ApiResponse.ok(null, "Matter deleted."));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyAuthority('view_all', 'edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Object>>> timeline(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }
}

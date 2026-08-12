package in.tubalaw.courtos.modules.filings.controller;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.common.util.PagedApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.documents.entity.Document;
import in.tubalaw.courtos.modules.documents.repository.DocumentRepository;
import in.tubalaw.courtos.modules.filings.entity.Filing;
import in.tubalaw.courtos.modules.filings.repository.FilingRepository;
import in.tubalaw.courtos.modules.filings.service.FilingService;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.notifications.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/filings")
@RequiredArgsConstructor
public class FilingController {
    private final FilingRepository repo;
    private final FilingService filingService;
    private final DocumentRepository docRepo;
    private final MatterRepository matterRepo;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    @GetMapping
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_filings', 'ROLE_ADMIN')")
    public ResponseEntity<PagedApiResponse<Filing>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order) {
        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC, sort));
        Page<Filing> result = filingService.list(status, search, pageable);
        return ResponseEntity.ok(PagedApiResponse.of(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('view_all', 'manage_filings', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Filing>> getById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.ok(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Filing", id))));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('manage_filings', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Filing>> create(@RequestBody Filing f) {
        f.setTenantId(TENANT);
        Filing saved = repo.save(f);
        notificationService.sendNotification(TENANT, null, "Filing Created",
                "Filing: " + saved.getTitle() + " has been added.", "info", "/app/filings");

        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Filing Created", "Filing", saved.getId().toString(),
                "Created filing: " + saved.getTitle(), request.getRemoteAddr(), "LOW");

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved, "Filing created!"));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('manage_filings', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Filing>> update(@PathVariable Long id, @RequestBody Filing updates) {
        Filing existing = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Filing", id));
        String previousStage = existing.getStage();
        boolean wasFiled = "Filed".equalsIgnoreCase(previousStage);
        if (updates.getTitle() != null)
            existing.setTitle(updates.getTitle());
        if (updates.getMatterId() != null)
            existing.setMatterId(updates.getMatterId());
        if (updates.getMatterTitle() != null)
            existing.setMatterTitle(updates.getMatterTitle());
        if (updates.getCourt() != null)
            existing.setCourt(updates.getCourt());
        if (updates.getFilingType() != null)
            existing.setFilingType(updates.getFilingType());
        if (updates.getAdvocate() != null)
            existing.setAdvocate(updates.getAdvocate());
        if (updates.getStatus() != null)
            existing.setStatus(updates.getStatus());
        if (updates.getStage() != null)
            existing.setStage(updates.getStage());
        if (updates.getFiledDate() != null)
            existing.setFiledDate(updates.getFiledDate());
        if (updates.getDueDate() != null)
            existing.setDueDate(updates.getDueDate());
        if (updates.getNotes() != null)
            existing.setNotes(updates.getNotes());
        if (updates.getS3Url() != null)
            existing.setS3Url(updates.getS3Url());
        Filing saved = repo.save(existing);

        boolean isNowFiled = "Filed".equalsIgnoreCase(saved.getStage());
        if (isNowFiled && !wasFiled) {
            linkFilingDocumentToMatter(saved);
            notificationService.sendNotification(TENANT, null, "Document Filed",
                    "Filing: " + saved.getTitle() + " has been marked Filed.", "info", "/app/filings");
        } else if ("Defects Raised".equalsIgnoreCase(saved.getStage())) {
            notificationService.sendNotification(TENANT, null, "Filing Defects Raised",
                    "Filing: " + saved.getTitle() + " has defects raised.", "warning", "/app/filings");
        }

        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        String stageInfo = (previousStage != null && !previousStage.equals(saved.getStage()))
                ? " (stage: " + previousStage + " → " + saved.getStage() + ")"
                : "";
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Filing Updated", "Filing", saved.getId().toString(),
                "Updated filing: " + saved.getTitle() + stageInfo, request.getRemoteAddr(), "LOW");

        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    /**
     * Creates the matter-linked Document record for a filing's attached file once
     * that filing reaches the "Filed" stage.
     */
    private void linkFilingDocumentToMatter(Filing filing) {
        if (filing.getS3Url() == null || filing.getMatterId() == null)
            return;
        if (docRepo.findFirstByS3Url(filing.getS3Url()).isPresent())
            return; // already linked

        Matter matter = matterRepo.findById(filing.getMatterId()).orElse(null);

        String fileName = filing.getTitle() != null
                ? filing.getTitle().replaceFirst("(?i)^Task Submission:\\s*", "").trim()
                : filing.getS3Url().substring(filing.getS3Url().lastIndexOf('/') + 1);

        Document doc = Document.builder()
                .name(fileName)
                .docType(filing.getFilingType())
                .matterId(filing.getMatterId())
                .clientId(matter != null ? matter.getClientId() : null)
                .clientName(matter != null ? matter.getClientName() : null)
                .s3Url(filing.getS3Url())
                .tags(new String[] { "Filing" })
                .uploadedBy(filing.getAdvocate())
                .build();
        doc.setTenantId(TENANT);
        docRepo.save(doc);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('manage_filings', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Filing filing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filing", id));

        if (filing.getS3Url() != null) {
            docRepo.findFirstByS3Url(filing.getS3Url())
                    .ifPresent(docRepo::delete);
        }

        repo.deleteById(id);

        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Filing Deleted", "Filing", id.toString(),
                "Deleted filing: " + filing.getTitle(), request.getRemoteAddr(), "MEDIUM");

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

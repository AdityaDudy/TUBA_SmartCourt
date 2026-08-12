package in.tubalaw.courtos.modules.tracker.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.tracker.dto.*;
import in.tubalaw.courtos.modules.tracker.entity.CaseOrder;
import in.tubalaw.courtos.modules.tracker.repository.CaseOrderRepository;
import in.tubalaw.courtos.modules.tracker.repository.TrackedCaseRepository;
import in.tubalaw.courtos.modules.tracker.service.TrackerService;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.tracker.storage.OrderFileStorage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/tracker")
@RequiredArgsConstructor
@Validated
public class TrackerController {

    private final TrackerService trackerService;
    private final TrackedCaseRepository trackedCaseRepo;
    private final CaseOrderRepository orderRepo;
    private final MatterRepository matterRepository;
    private final in.tubalaw.courtos.modules.documents.repository.DocumentRepository docRepo;
    private final OrderFileStorage orderFileStorage;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    // ══════════════════════════════════════════════════════════════
    // SEARCH — returns 200 (cached) or 202 (queued)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ScrapeJobStatusResponse>> search(
            @RequestParam String cnr,
            HttpServletRequest request) {

        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        String tenantId = user != null ? "default" : "default"; // extend for multi-tenancy
        Long userId = user != null ? user.getUserId() : null;
        String email = user != null ? user.getEmail() : "anonymous";
        String ip = request.getRemoteAddr();

        ScrapeJobStatusResponse resp = trackerService.searchByCnr(cnr, tenantId, userId, email, ip);

        boolean isAsync = "PENDING".equals(resp.getStatus()) || "RUNNING".equals(resp.getStatus());
        HttpStatus status = isAsync ? HttpStatus.ACCEPTED : HttpStatus.OK;

        return ResponseEntity.status(status).body(ApiResponse.ok(resp));
    }

    // ══════════════════════════════════════════════════════════════
    // CASE-NUMBER RESOLVER SEARCH — resolves CNR & enqueues job
    // ══════════════════════════════════════════════════════════════

    /**
     * GET /api/tracker/search-by-case-number?caseType=CS(OS)&number=403&year=2026
     */
    @GetMapping("/search-by-case-number")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ScrapeJobStatusResponse>> searchByCaseNumber(
            @RequestParam(required = false) String caseType,
            @RequestParam String number,
            @RequestParam String year,
            HttpServletRequest request) {

        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        String tenantId = user != null ? "default" : "default";
        Long userId = user != null ? user.getUserId() : null;
        String email = user != null ? user.getEmail() : "anonymous";
        String ip = request.getRemoteAddr();

        ScrapeJobStatusResponse resp = trackerService.searchByCaseNumber(
                caseType, number, year, tenantId, userId, email, ip);

        boolean isAsync = "PENDING".equals(resp.getStatus()) || "RUNNING".equals(resp.getStatus());
        HttpStatus status = isAsync ? HttpStatus.ACCEPTED : HttpStatus.OK;

        return ResponseEntity.status(status).body(ApiResponse.ok(resp));
    }

    // ══════════════════════════════════════════════════════════════
    // CASE-NUMBER SEARCH — returns candidate list synchronously
    // ══════════════════════════════════════════════════════════════

    /**
     * GET /api/tracker/search-advanced?type=CASE_NUMBER&value=CS(OS) 403/2026
     */
    @GetMapping("/search-advanced")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<CaseSearchResultDto>>> searchAdvanced(
            @RequestParam String type,
            @RequestParam String value) {

        if (!"CASE_NUMBER".equalsIgnoreCase(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported search type: " + type + ". Only CASE_NUMBER is currently supported.");
        }
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value must not be blank.");
        }

        try {
            String query = value.trim();
            List<in.tubalaw.courtos.modules.matters.entity.Matter> matters = matterRepository
                    .findByTenantIdAndCaseNoMatching("default", query);
            if (matters.isEmpty()) {
                matters = matterRepository.findByCaseNoLike("default", query);
            }

            List<CaseSearchResultDto> results = new java.util.ArrayList<>();
            for (in.tubalaw.courtos.modules.matters.entity.Matter m : matters) {
                if (m.getCnrNumber() != null && !m.getCnrNumber().isBlank()) {
                    results.add(CaseSearchResultDto.builder()
                            .cnr(m.getCnrNumber().trim().toUpperCase())
                            .caseType(m.getType() != null ? m.getType() : "Matter")
                            .courtName(m.getCourt())
                            .petitioners(new String[] { m.getTitle() })
                            .respondents(new String[] { m.getOppositeParty() != null ? m.getOppositeParty() : "—" })
                            .build());
                }
            }

            return ResponseEntity.ok(ApiResponse.ok(results));
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.warn("[Tracker] search-advanced failed for value='{}': {}", value, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Case search temporarily unavailable: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // JOB STATUS POLLING
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ScrapeJobStatusResponse>> getJobStatus(@PathVariable Long jobId) {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        String tenantId = "default";
        Long userId = user != null ? user.getUserId() : null;
        return ResponseEntity.ok(ApiResponse.ok(trackerService.getJobStatus(jobId, tenantId, userId)));
    }

    // ══════════════════════════════════════════════════════════════
    // GET PERSISTED CASE DETAIL
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/{cnr}")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CaseDetailResponse>> getCaseDetail(@PathVariable String cnr) {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        String tenantId = "default";
        Long userId = user != null ? user.getUserId() : null;
        return ResponseEntity.ok(ApiResponse.ok(trackerService.getCaseDetail(cnr.toUpperCase(), tenantId, userId)));
    }

    // ══════════════════════════════════════════════════════════════
    // FORCE REFRESH
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/{cnr}/refresh")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ScrapeJobStatusResponse>> refresh(
            @PathVariable String cnr,
            HttpServletRequest request) {

        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        String tenantId = "default";
        Long userId = user != null ? user.getUserId() : null;
        String email = user != null ? user.getEmail() : "anonymous";

        return ResponseEntity.accepted().body(
                ApiResponse.ok(trackerService.refreshCase(cnr.toUpperCase(), tenantId, userId, email,
                        request.getRemoteAddr())));
    }

    // ══════════════════════════════════════════════════════════════
    // DOWNLOAD SINGLE ORDER PDF (stream from S3)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/{cnr}/orders/{orderId}/download")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_docs', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> downloadOrder(
            @PathVariable String cnr,
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "attachment") String mode) {

        CaseOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        byte[] bytes = null;
        if (order.getS3Key() != null && !order.getS3Key().isBlank()) {
            try {
                bytes = orderFileStorage.retrieve(order.getS3Key());
            } catch (Exception ignored) {
            }
        }

        // On-demand fetch from eCourts if not locally stored yet
        if (bytes == null || bytes.length == 0) {
            bytes = trackerService.fetchOrderPdfBytesOnDemand(cnr, order);
        }

        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Document file is currently processing from court. Please try force refresh or try again shortly.");
        }

        try {
            String dispositionType = "inline".equalsIgnoreCase(mode) ? "inline" : "attachment";
            SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
            auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                    user != null ? user.getEmail() : "anonymous",
                    "Order Downloaded", "CaseOrder", orderId.toString(),
                    "Downloaded order #" + orderId + " for CNR: " + cnr, request.getRemoteAddr(), "LOW");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            dispositionType + "; filename=\"Order-" + orderId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("[Tracker] Failed to read order {}: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Download failed");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // SAVE ORDER DOCUMENT TO LINKED MATTER'S VAULT FOLDER
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/{cnr}/orders/{orderId}/save-to-matter")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveOrderToMatterVault(
            @PathVariable String cnr,
            @PathVariable Long orderId) {

        var tc = trackedCaseRepo.findByCnrAndTenantId(cnr.toUpperCase(), "default")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        if (tc.getMatterId() == null) {
            // Try auto-resolving linked matter by case number
            var match = matterRepository.findFirstByTenantIdAndCaseNoIgnoreCase("default", tc.getCnr()).orElse(null);
            if (match == null && tc.getRegistrationNo() != null) {
                match = matterRepository.findFirstByTenantIdAndCaseNoIgnoreCase("default", tc.getRegistrationNo())
                        .orElse(null);
            }
            if (match == null && tc.getFilingNo() != null) {
                match = matterRepository.findFirstByTenantIdAndCaseNoIgnoreCase("default", tc.getFilingNo())
                        .orElse(null);
            }
            if (match != null) {
                tc.setMatterId(match.getId());
                trackedCaseRepo.save(tc);
            }
        }

        if (tc.getMatterId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This case is not linked to a Matter yet. Please link a Matter first.");
        }

        CaseOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        var matter = matterRepository.findById(tc.getMatterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked matter not found"));

        String docName = "Order " + (order.getOrderDate() != null ? order.getOrderDate() : orderId) + ".pdf";

        // Check if document already exists in this matter folder
        var existing = docRepo.findAllByTenantIdAndMatterId("default", matter.getId()).stream()
                .filter(d -> d.getName() != null && d.getName().equalsIgnoreCase(docName))
                .findFirst();

        String safeCnr = cnr.replaceAll("[^a-zA-Z0-9]", "");
        String filename = "Order_" + safeCnr + "_" + (order.getOrderDate() != null ? order.getOrderDate() : orderId)
                + ".pdf";

        // Save PDF file locally to uploads/ecourtfiles directory
        try {
            java.nio.file.Path targetDir = java.nio.file.Paths.get("..").toAbsolutePath().normalize().resolve("uploads")
                    .resolve("ecourtfiles");
            if (!java.nio.file.Files.exists(targetDir)) {
                java.nio.file.Files.createDirectories(targetDir);
            }
            java.nio.file.Path destination = targetDir.resolve(filename);

            // Fetch PDF byte content
            byte[] pdfBytes = null;
            if (order.getS3Key() != null && !order.getS3Key().isBlank()) {
                try {
                    pdfBytes = orderFileStorage.retrieve(order.getS3Key());
                } catch (Exception ignored) {
                }
            }
            if (pdfBytes == null || pdfBytes.length == 0) {
                pdfBytes = trackerService.fetchOrderPdfBytesOnDemand(cnr, order);
            }

            if (pdfBytes != null && pdfBytes.length > 0) {
                java.nio.file.Files.write(destination, pdfBytes, java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            log.warn("[Tracker] Failed to write order file to uploads/ecourtfiles: {}", e.getMessage());
        }

        String localFileUrl = "/uploads/ecourtfiles/" + filename;

        if (existing.isPresent()) {
            var exDoc = existing.get();
            if (!localFileUrl.equals(exDoc.getS3Url())) {
                exDoc.setS3Url(localFileUrl);
                docRepo.save(exDoc);
            }
            return ResponseEntity
                    .ok(ApiResponse.ok(Map.of("saved", true, "alreadyExisted", true, "docId", exDoc.getId()),
                            "Document is already saved in the matter folder."));
        }

        var doc = in.tubalaw.courtos.modules.documents.entity.Document.builder()
                .name(docName)
                .docType(order.getOrderType() != null ? order.getOrderType() : "Court Order")
                .matterId(matter.getId())
                .clientId(matter.getClientId())
                .clientName(matter.getClientName())
                .fileSize(order.getFileSize() != null ? order.getFileSize() : 1024L * 100)
                .mimeType("application/pdf")
                .s3Url(localFileUrl)
                .tags(new String[] { "Court Order", cnr })
                .uploadedBy("eCourts Tracker")
                .build();
        doc.setTenantId("default");

        docRepo.save(doc);
        log.info("[Tracker] Saved order {} as Document for Matter ID {}", orderId, matter.getId());
        SecurityScopeContext.UserSecurityDetails saveUser = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, saveUser != null ? saveUser.getUserId() : null,
                saveUser != null ? saveUser.getEmail() : "anonymous",
                "Order Saved to Matter", "CaseOrder", orderId.toString(),
                "Saved order #" + orderId + " (CNR: " + cnr + ") to matter: " + matter.getTitle(),
                request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(Map.of("saved", true, "docId", doc.getId()),
                "Order saved to Matter '" + matter.getTitle() + "' document folder."));
    }

    // ══════════════════════════════════════════════════════════════
    // DOWNLOAD ALL ORDERS AS ZIP (streaming, no full buffer)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/{cnr}/download-all")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_docs', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> downloadAll(@PathVariable String cnr) {
        var tc = trackedCaseRepo.findByCnrAndTenantId(cnr.toUpperCase(), "default")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        List<CaseOrder> orders = orderRepo.findByTrackedCaseIdOrderByOrderDateDesc(tc.getId());
        if (orders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No documents recorded for this case");
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int addedEntries = 0;
            try (ZipOutputStream zip = new ZipOutputStream(bos)) {
                for (CaseOrder o : orders) {
                    if (o.getS3Key() == null || o.getS3Key().isBlank()) {
                        log.info("[Tracker] Skipping order {} — S3 key not present", o.getId());
                        continue;
                    }
                    try {
                        byte[] bytes = orderFileStorage.retrieve(o.getS3Key());
                        if (bytes != null && bytes.length > 0) {
                            String entryName = "Order-" + (o.getOrderDate() != null ? o.getOrderDate() : o.getId())
                                    + ".pdf";
                            zip.putNextEntry(new ZipEntry(entryName));
                            zip.write(bytes);
                            zip.closeEntry();
                            addedEntries++;
                        }
                    } catch (Exception e) {
                        log.warn("[Tracker] Document retrieval failed for key {}: {}", o.getS3Key(), e.getMessage());
                    }
                }
                zip.finish();
            }

            if (addedEntries == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order PDFs are still downloading from eCourts. Please try again in a few seconds.");
            }

            byte[] zipBytes = bos.toByteArray();
            SecurityScopeContext.UserSecurityDetails dlUser = SecurityScopeContext.getCurrentUser();
            auditLogService.log(TENANT, dlUser != null ? dlUser.getUserId() : null,
                    dlUser != null ? dlUser.getEmail() : "anonymous",
                    "Orders Downloaded (ZIP)", "TrackedCase", cnr,
                    "Downloaded all orders as ZIP for CNR: " + cnr + " (" + addedEntries + " files)",
                    request.getRemoteAddr(), "LOW");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"Orders-" + cnr + ".zip\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipBytes.length))
                    .body(zipBytes);
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("[Tracker] Error generating order ZIP for CNR {}: {}", cnr, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Zip creation failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // EXPORT CASE SUMMARY PDF
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/{cnr}/export")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'export_billing', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> export(
            @PathVariable String cnr,
            @RequestParam(defaultValue = "pdf") String format) {

        if (!"pdf".equalsIgnoreCase(format)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only format=pdf is currently supported");
        }

        byte[] pdf = trackerService.exportSummaryPdf(cnr.toUpperCase(), TENANT);
        SecurityScopeContext.UserSecurityDetails expUser = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, expUser != null ? expUser.getUserId() : null,
                expUser != null ? expUser.getEmail() : "anonymous",
                "Case Exported", "TrackedCase", cnr.toUpperCase(),
                "Exported case summary PDF for CNR: " + cnr, request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"CaseSummary-" + cnr + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ══════════════════════════════════════════════════════════════
    // ALERT TOGGLE
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/{cnr}/alert")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleAlert(
            @PathVariable String cnr,
            @RequestBody Map<String, Boolean> body) {

        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        Long userId = user != null ? user.getUserId() : null;
        if (userId == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");

        boolean enable = Boolean.TRUE.equals(body.get("enabled"));
        boolean result = trackerService.toggleAlert(cnr.toUpperCase(), "default", userId, enable);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("alertActive", result)));
    }

    // ══════════════════════════════════════════════════════════════
    // RECENT SEARCHES
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<RecentSearchDto>>> getHistory() {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        Long userId = user != null ? user.getUserId() : null;
        if (userId == null)
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        return ResponseEntity.ok(ApiResponse.ok(trackerService.getRecentSearches("default", userId)));
    }

    // ══════════════════════════════════════════════════════════════
    // SUGGEST MATTER LINK (returns best match, not auto-linked)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/{cnr}/suggest-matter")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'view_all', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestMatter(@PathVariable String cnr) {
        var tc = trackedCaseRepo.findByCnrAndTenantId(cnr.toUpperCase(), "default").orElse(null);
        if (tc == null)
            return ResponseEntity.ok(ApiResponse.ok(null));

        // Search existing matters by case number for a suggestion
        var matches = matterRepository.search("default", cnr);
        if (matches.isEmpty() && tc.getFilingNo() != null) {
            matches = matterRepository.search("default", tc.getFilingNo());
        }

        if (matches.isEmpty())
            return ResponseEntity.ok(ApiResponse.ok(null));

        var m = matches.get(0);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "matterId", m.getId(),
                "matterTitle", m.getTitle(),
                "confidence", "HIGH")));
    }

    // ══════════════════════════════════════════════════════════════
    // LINK TO MATTER (after user confirms)
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/{cnr}/link-matter")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> linkToMatter(
            @PathVariable String cnr,
            @RequestBody Map<String, Long> body) {

        Long matterId = body.get("matterId");
        if (matterId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "matterId required");
        trackerService.linkToMatter(cnr.toUpperCase(), matterId, "default");
        return ResponseEntity.ok(ApiResponse.ok(null, "Case linked to matter."));
    }

    // ══════════════════════════════════════════════════════════════
    // BULK LINK — accepts [{ cnr, matterId }], links each in a loop
    // ══════════════════════════════════════════════════════════════

    /**
     * Bulk CNR → Matter linker for the onboarding screen.
     *
     * POST /api/tracker/bulk-link
     * Body: [{ "cnr": "MHXXXXXXXX", "matterId": 42 }, ...]
     *
     * For each entry, calls the existing trackerService.linkToMatter().
     * Returns a per-row result array so the UI can show which rows succeeded
     * and which failed (e.g. CNR not yet tracked, matter not found).
     */
    @PostMapping("/bulk-link")
    @PreAuthorize("hasAnyAuthority('use_court_tracker', 'edit_matters', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> bulkLink(
            @RequestBody List<Map<String, Object>> entries) {

        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (Map<String, Object> entry : entries) {
            String cnr = entry.get("cnr") instanceof String s ? s.trim().toUpperCase() : null;
            Long matterId = entry.get("matterId") instanceof Number n ? n.longValue() : null;

            if (cnr == null || matterId == null) {
                results.add(Map.of("cnr", String.valueOf(cnr), "matterId", String.valueOf(matterId),
                        "success", false, "error", "cnr and matterId are required"));
                continue;
            }

            try {
                trackerService.linkToMatter(cnr, matterId, "default");
                results.add(Map.of("cnr", cnr, "matterId", matterId, "success", true));
            } catch (Exception e) {
                results.add(Map.of("cnr", cnr, "matterId", matterId,
                        "success", false, "error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(results, "Bulk link complete."));
    }
}

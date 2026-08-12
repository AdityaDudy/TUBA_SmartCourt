package in.tubalaw.courtos.modules.documents.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.documents.entity.Document;
import in.tubalaw.courtos.modules.documents.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private static final String TENANT = "default";

    @GetMapping("/folders/client")
    @PreAuthorize("hasAnyAuthority('view_all', 'view_docs', 'edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> clientFolders() {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getClientFolders()));
    }

    @GetMapping("/folders/matter")
    @PreAuthorize("hasAnyAuthority('view_all', 'view_docs', 'edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> matterFolders() {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getMatterFolders()));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyAuthority('view_all', 'view_docs', 'edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> recent() {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getRecentDocuments()));
    }

    @GetMapping("/folders/client/{clientId}/contents")
    @PreAuthorize("hasAnyAuthority('view_all', 'view_docs', 'edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClientFolderContents(@PathVariable Long clientId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getClientFolderContents(clientId)));
    }

    @GetMapping("/folders/matter/{matterId}/contents")
    @PreAuthorize("hasAnyAuthority('view_all', 'view_docs', 'edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMatterFolderContents(@PathVariable Long matterId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getMatterFolderContents(matterId)));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Document>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "matterId", required = false) Long matterId,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "tags", required = false) String tags) {

        String[] parsedTags = tags != null && !tags.isBlank()
                ? tags.split(",")
                : new String[0];
        for (int i = 0; i < parsedTags.length; i++) {
            parsedTags[i] = parsedTags[i].trim();
        }

        Document doc = documentService.uploadDocument(file, clientId, matterId, docType, parsedTags);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Document Uploaded", "Document", doc.getId().toString(),
                "Uploaded document: " + doc.getName() + " (type: " + docType + ")", request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(doc, "Document uploaded and OCR queued!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('delete_docs', 'edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Document Deleted", "Document", id.toString(),
                "Deleted document #" + id, request.getRemoteAddr(), "MEDIUM");
        return ResponseEntity.ok(ApiResponse.ok(null, "Document deleted successfully"));
    }

    @PostMapping("/create-mock")
    @PreAuthorize("hasAnyAuthority('edit_docs', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Document>> createMock(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        Long matterId = payload.get("matterId") != null ? Long.valueOf(payload.get("matterId").toString()) : null;
        String docType = (String) payload.get("docType");
        Document doc = documentService.createMockDocument(name, matterId, docType);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Document Linked", "Document", doc.getId().toString(),
                "Linked document: " + name + " to matter #" + matterId, request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(doc, "Mock document created!"));
    }

    @PostMapping("/upload-for-filing")
    @PreAuthorize("hasAnyAuthority('edit_docs', 'manage_filings', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadForFiling(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = documentService.storeFileForFiling(file);
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log(TENANT, user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "system",
                "Filing Attachment Uploaded", "Document", null,
                "Uploaded filing attachment: " + file.getOriginalFilename(), request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(result, "File attached to filing."));
    }
}
